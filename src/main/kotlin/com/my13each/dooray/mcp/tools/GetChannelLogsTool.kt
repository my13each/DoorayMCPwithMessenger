package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.ChannelLogsResponseData
import com.my13each.dooray.mcp.types.ChannelMessage
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_LOG_SIZE = 50
private const val MAX_LOG_SIZE = 1000

fun getChannelLogsTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_channel_logs",
        description = """
            두레이 메신저 채널(대화방)의 메시지 목록을 조회합니다.

            ⚠️ 제약사항:
            - 최신 메시지부터 최대 size개만 조회됩니다 (size 최대 1000, 기본 50).
            - 페이지네이션을 지원하지 않아 그보다 오래된 메시지는 조회할 수 없습니다.
            - 공식 API 문서에 기재되지 않은 엔드포인트입니다.

            📝 응답:
            - messages는 seq 오름차순(오래된 순 → 최신 순)으로 정렬됩니다.
            - include_sender_names=true(기본)이면 senderName에 발신자 이름을 채웁니다.
              봇 메시지는 sender.type=app, sender.app.appId로 표시됩니다.
            - REPLY(답장) 메시지는 text에 답장 본문만 풀어서 넣고, 원본 JSON은 rawText에 둡니다.
            - 첨부 파일이 있으면 file 필드에 정보가 포함됩니다 (업로드 후 약 2주 뒤 만료).
            - 메시지 id(log-id)는 update_message / delete_message / reply_message / create_thread_from_message에 사용합니다.
            - 스레드(글타래)는 스레드 채널 ID를 channelId로 넘기면 읽을 수 있습니다.
              단, 부모 메시지에는 스레드 정보가 없으므로 스레드 채널 ID는 create_thread 계열 도구의 응답으로만 알 수 있습니다.
            - flags=DELETED_ROOT는 삭제됐지만 스레드가 남아 있는 메시지입니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("channelId", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("메시지를 조회할 채널 ID (dooray_messenger_get_simple_channels로 확인)"))
                })
                put("size", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("조회할 최신 메시지 개수 (1~$MAX_LOG_SIZE, 기본값: $DEFAULT_LOG_SIZE)"))
                    put("minimum", JsonPrimitive(1))
                    put("maximum", JsonPrimitive(MAX_LOG_SIZE))
                    put("default", JsonPrimitive(DEFAULT_LOG_SIZE))
                })
                put("include_sender_names", buildJsonObject {
                    put("type", JsonPrimitive("boolean"))
                    put("description", JsonPrimitive("발신자 이름(senderName)을 채울지 여부 (기본값: true)"))
                    put("default", JsonPrimitive(true))
                })
            },
            required = listOf("channelId")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getChannelLogsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    // 멤버 ID → 이름 캐시 (서버 수명 동안 유지, 조회 실패는 캐시하지 않음)
    val memberNameCache = ConcurrentHashMap<String, String>()

    suspend fun resolveName(memberId: String): String? =
        memberNameCache[memberId]
            ?: runCatching { doorayClient.getMember(memberId).result?.name }.getOrNull()
                ?.also { memberNameCache[memberId] = it }

    return { request ->
        try {
            val channelId = request.arguments["channelId"]?.jsonPrimitive?.content
            val size = (request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: DEFAULT_LOG_SIZE)
                .coerceIn(1, MAX_LOG_SIZE)
            val includeSenderNames = request.arguments["include_sender_names"]?.jsonPrimitive?.booleanOrNull ?: true

            when {
                channelId.isNullOrBlank() -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "channelId 파라미터가 필요합니다.",
                        code = "MISSING_CHANNEL_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val response = doorayClient.getChannelLogs(channelId, size)

                    if (response.header.isSuccessful) {
                        val messages = response.result
                            .sortedBy { it.seq ?: Long.MIN_VALUE }
                            .map { unwrapReplyText(it) }
                            .map { message ->
                                val memberId = message.sender?.member?.organizationMemberId
                                if (includeSenderNames && memberId != null) {
                                    message.copy(senderName = resolveName(memberId))
                                } else message
                            }
                        val successResponse = ToolSuccessResponse(
                            data = ChannelLogsResponseData(
                                channelId = channelId,
                                messages = messages,
                                count = messages.size,
                                requestedSize = size
                            ),
                            message = "💬 채널 메시지 ${messages.size}개를 조회했습니다."
                        )

                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = response.header.resultMessage ?: "채널 메시지 조회에 실패했습니다.",
                            code = "DOORAY_API_${response.header.resultCode}"
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
                message = "채널 메시지 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_CHANNEL_LOGS_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}

/** REPLY 메시지의 text({"type":0,"text":"..."})에서 답장 본문만 꺼냄. 실패 시 원본 유지 */
private fun unwrapReplyText(message: ChannelMessage): ChannelMessage {
    if (message.type != "REPLY") return message
    val raw = message.text ?: return message
    val inner = runCatching {
        JsonUtils.json.parseToJsonElement(raw).jsonObject["text"]?.jsonPrimitive?.contentOrNull
    }.getOrNull() ?: return message
    return message.copy(text = inner, rawText = raw)
}
