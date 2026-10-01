package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.MessageSendResponse
import com.my13each.dooray.mcp.types.MessageSendResponseData
import com.my13each.dooray.mcp.types.MessageTextRequest
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

private fun messageTargetInputSchema(textDescription: String) = Tool.Input(
    properties = buildJsonObject {
        putJsonObject("channel_id") {
            put("type", "string")
            put("description", "대상 메시지가 있는 채널 ID")
        }
        putJsonObject("log_id") {
            put("type", "string")
            put("description", "대상 메시지의 log-id (dooray_messenger_get_channel_logs 결과의 id)")
        }
        putJsonObject("text") {
            put("type", "string")
            put("description", textDescription)
        }
    },
    required = listOf("channel_id", "log_id", "text")
)

fun replyMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_reply_message",
        description = "두레이 메신저 채널의 특정 메시지(log-id)에 답장(reply)을 보냅니다. 답장은 원본 메시지를 인용한 형태로 같은 채널에 전송됩니다.",
        inputSchema = messageTargetInputSchema("답장 메시지 내용"),
        outputSchema = null,
        annotations = null
    )
}

fun createThreadFromMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_create_thread_from_message",
        description = """
            두레이 메신저 채널의 기존 메시지(log-id)를 기반으로 글타래(스레드)를 생성하고 메시지를 전송합니다.
            새 메시지와 스레드를 동시에 만들려면 dooray_messenger_create_thread를 사용하세요.
            응답의 sentChannelId(스레드 채널 ID)로 dooray_messenger_send_channel_message를 호출하면 같은 스레드에 이어서 보낼 수 있습니다.
        """.trimIndent(),
        inputSchema = messageTargetInputSchema("스레드에 보낼 메시지 내용"),
        outputSchema = null,
        annotations = null
    )
}

fun replyMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    messageTargetHandler(
        actionName = "답장 전송",
        errorCode = "REPLY_MESSAGE"
    ) { channelId, logId, text ->
        doorayClient.replyToMessage(channelId, logId, MessageTextRequest(text))
    }

fun createThreadFromMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    messageTargetHandler(
        actionName = "스레드 생성 및 전송",
        errorCode = "CREATE_THREAD_FROM_MESSAGE"
    ) { channelId, logId, text ->
        doorayClient.createThreadFromMessage(channelId, logId, MessageTextRequest(text))
    }

private fun messageTargetHandler(
    actionName: String,
    errorCode: String,
    send: suspend (channelId: String, logId: String, text: String) -> MessageSendResponse
): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val logId = request.arguments["log_id"]?.jsonPrimitive?.content
            val text = request.arguments["text"]?.jsonPrimitive?.content

            val missing = when {
                channelId.isNullOrBlank() -> "channel_id"
                logId.isNullOrBlank() -> "log_id"
                text.isNullOrBlank() -> "text"
                else -> null
            }

            if (missing != null) {
                val errorResponse = ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "$missing 파라미터가 필요합니다.",
                    code = "MISSING_${missing.uppercase()}"
                ).toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            } else {
                val response = send(channelId!!, logId!!, text!!)

                if (response.header.isSuccessful) {
                    val successResponse = ToolSuccessResponse(
                        data = MessageSendResponseData(
                            channelId = channelId,
                            targetLogId = logId,
                            logId = response.result?.id,
                            sentChannelId = response.result?.channelId,
                            sentText = text
                        ),
                        message = "$actionName 이(가) 완료되었습니다."
                    )
                    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
                } else {
                    val errorResponse = ToolException(
                        type = ToolException.API_ERROR,
                        message = "$actionName 실패: ${response.header.resultMessage}",
                        code = "${errorCode}_FAILED"
                    ).toErrorResponse()

                    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                }
            }
        } catch (e: ToolException) {
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "$actionName 중 오류가 발생했습니다: ${e.message}",
                code = "${errorCode}_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
