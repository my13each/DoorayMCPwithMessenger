package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ChannelMessageResponseData
import com.bifos.dooray.mcp.types.SendChannelMessageRequest
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun sendChannelMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_send_channel_message",
        description = "두레이 메신저 채널에 메시지를 전송합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "메시지를 전송할 채널의 ID (dooray_messenger_get_channels로 조회 가능)")
                }
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "전송할 메시지 내용")
                }
                putJsonObject("message_type") {
                    put("type", "string")
                    put("description", "메시지 타입 (기본값: text)")
                    put("default", "text")
                }
            },
            required = listOf("channel_id", "text")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun sendChannelMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
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
                    val sendMessageRequest = SendChannelMessageRequest(
                        text = text,
                        messageType = messageType
                    )

                    val response = doorayClient.sendChannelMessage(channelId, sendMessageRequest)

                    if (response.header.isSuccessful) {
                        val data = ChannelMessageResponseData(
                            channelId = channelId,
                            sentText = text,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "채널 메시지가 성공적으로 전송되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "채널 메시지 전송 실패: ${response.header.resultMessage}",
                            code = "SEND_CHANNEL_MESSAGE_FAILED"
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
                message = "채널 메시지 전송 중 오류가 발생했습니다: ${e.message}",
                code = "SEND_CHANNEL_MESSAGE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}