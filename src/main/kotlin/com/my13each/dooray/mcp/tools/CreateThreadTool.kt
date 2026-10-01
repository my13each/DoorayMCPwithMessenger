package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.CreateThreadRequest
import com.my13each.dooray.mcp.types.CreateThreadResponseData
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

fun createThreadTool(): Tool {
    return Tool(
        name = "dooray_messenger_create_thread",
        description = "두레이 메신저 채널에 스레드를 생성하고 첫 메시지를 전송합니다. 응답의 threadId(스레드 채널 ID)로 dooray_messenger_send_channel_message를 호출하면 같은 스레드에 이어서 보낼 수 있습니다. 기존 메시지에 스레드를 달려면 dooray_messenger_create_thread_from_message를 사용하세요.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "스레드를 생성할 채널의 ID")
                }
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "대화방(채널)에 보낼 메시지 내용. 이 메시지에 스레드가 달립니다.")
                }
                putJsonObject("thread_text") {
                    put("type", "string")
                    put("description", "스레드(글타래)에 보낼 첫 메시지 (선택). 생략하면 스레드 첫 메시지가 \"#\"으로 들어갑니다.")
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

fun createThreadHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val text = request.arguments["text"]?.jsonPrimitive?.content
            val threadText = request.arguments["thread_text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
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
                    val createThreadRequest = CreateThreadRequest(
                        text = text,
                        threadText = threadText,
                        messageType = messageType
                    )

                    val response = doorayClient.createThreadAndSend(channelId, createThreadRequest)

                    if (response.header.isSuccessful && response.result != null) {
                        val data = CreateThreadResponseData(
                            channelId = channelId,
                            threadId = response.result.channelId,
                            logId = response.result.id,
                            sentText = text,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "스레드가 성공적으로 생성되고 메시지가 전송되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "스레드 생성 실패: ${response.header.resultMessage}",
                            code = "CREATE_THREAD_FAILED"
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
                message = "스레드 생성 중 오류가 발생했습니다: ${e.message}",
                code = "CREATE_THREAD_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}
