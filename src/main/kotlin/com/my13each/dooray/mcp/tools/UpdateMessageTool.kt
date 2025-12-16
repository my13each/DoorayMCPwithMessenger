package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.UpdateMessageRequest
import com.my13each.dooray.mcp.types.UpdateMessageResponseData
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

fun updateMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_update_message",
        description = "두레이 메신저 채널의 메시지를 수정합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "메시지가 있는 채널의 ID")
                }
                putJsonObject("log_id") {
                    put("type", "string")
                    put("description", "수정할 메시지의 로그 ID")
                }
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "수정할 메시지 내용")
                }
                putJsonObject("message_type") {
                    put("type", "string")
                    put("description", "메시지 타입 (기본값: text)")
                    put("default", "text")
                }
            },
            required = listOf("channel_id", "log_id", "text")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun updateMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val logId = request.arguments["log_id"]?.jsonPrimitive?.content
            val text = request.arguments["text"]?.jsonPrimitive?.content
            val messageType = request.arguments["message_type"]?.jsonPrimitive?.content ?: "text"

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
                text == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "text 파라미터가 필요합니다.",
                        code = "MISSING_TEXT"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val updateMessageRequest = UpdateMessageRequest(
                        text = text,
                        messageType = messageType
                    )

                    val response = doorayClient.updateChannelMessage(channelId, logId, updateMessageRequest)

                    if (response.header.isSuccessful) {
                        val data = UpdateMessageResponseData(
                            channelId = channelId,
                            logId = logId,
                            updatedText = text,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "메시지가 성공적으로 수정되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "메시지 수정 실패: ${response.header.resultMessage}",
                            code = "UPDATE_MESSAGE_FAILED"
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
                message = "메시지 수정 중 오류가 발생했습니다: ${e.message}",
                code = "UPDATE_MESSAGE_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}
