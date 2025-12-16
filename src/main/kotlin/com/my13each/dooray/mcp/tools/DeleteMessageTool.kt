package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.DeleteMessageResponseData
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun deleteMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_delete_message",
        description = "두레이 메신저 채널의 메시지를 삭제합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "메시지가 있는 채널의 ID")
                }
                putJsonObject("log_id") {
                    put("type", "string")
                    put("description", "삭제할 메시지의 로그 ID")
                }
            },
            required = listOf("channel_id", "log_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun deleteMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val logId = request.arguments["log_id"]?.jsonPrimitive?.content

            when {
                channelId == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "channel_id 파라미터가 필요합니다.",
                        code = "MISSING_CHANNEL_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                logId == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "log_id 파라미터가 필요합니다.",
                        code = "MISSING_LOG_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val response = doorayClient.deleteChannelMessage(channelId, logId)

                    if (response.header.isSuccessful) {
                        val data = DeleteMessageResponseData(
                            channelId = channelId,
                            logId = logId,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "메시지가 성공적으로 삭제되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "메시지 삭제 실패: ${response.header.resultMessage}",
                            code = "DELETE_MESSAGE_FAILED"
                        ).toErrorResponse()

                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                        )
                    }
                }
            }
        } catch (e: ToolException) {
            val errorResponse = e.toErrorResponse()
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "메시지 삭제 중 오류가 발생했습니다: ${e.message}",
                code = "DELETE_MESSAGE_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}
