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
import kotlinx.serialization.json.*

fun sendChannelMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_send_channel_message",
        description = "두레이 메신저 채널에 메시지를 전송합니다. 멘션 기능 지원: [@사용자명](dooray://조직ID/members/멤버ID \"member\") 또는 [@Channel](dooray://조직ID/channels/채널ID \"channel\")",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "메시지를 전송할 채널의 ID (dooray_messenger_get_channels로 조회 가능)")
                }
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "전송할 메시지 내용. 멘션 사용법: [@사용자명](dooray://조직ID/members/멤버ID \"member\") 또는 [@Channel](dooray://조직ID/channels/채널ID \"channel\")")
                }
                putJsonObject("mention_members") {
                    put("type", "array")
                    put("description", "멘션할 멤버 목록 (선택사항). 각 항목: {\"id\": \"멤버ID\", \"name\": \"멤버명\", \"organizationId\": \"조직ID\"}")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("id") {
                                put("type", "string")
                                put("description", "멤버 ID")
                            }
                            putJsonObject("name") {
                                put("type", "string") 
                                put("description", "멤버 이름")
                            }
                            putJsonObject("organizationId") {
                                put("type", "string")
                                put("description", "조직 ID")
                            }
                        }
                    }
                }
                putJsonObject("mention_all") {
                    put("type", "boolean")
                    put("description", "채널 전체 멘션 여부 (선택사항). true이면 [@Channel] 멘션 자동 추가")
                    put("default", false)
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
            val mentionMembers = request.arguments["mention_members"]?.jsonArray
            val mentionAll = request.arguments["mention_all"]?.jsonPrimitive?.boolean ?: false

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
                    // 멘션 기능 처리
                    var finalText = text
                    
                    // 텍스트에 이미 Dooray 멘션 형식이 포함되어 있는지 확인
                    val hasDoorayMention = finalText.contains("[@") && finalText.contains("](dooray://")
                    
                    // Claude가 생성한 멘션 텍스트를 이상적인 형식으로 재구성
                    if (hasDoorayMention) {
                        // 멘션 패턴을 찾아서 분리
                        val mentionRegex = Regex("""\[@[^\]]+\]\(dooray://[^\)]+\)""")
                        val mentions = mentionRegex.findAll(finalText).map { it.value }.toList()
                        
                        if (mentions.isNotEmpty()) {
                            // 멘션을 제거한 텍스트
                            var cleanText = finalText
                            mentions.forEach { mention ->
                                cleanText = cleanText?.replace(mention, "")?.trim() ?: ""
                            }
                            
                            // "さん、" 패턴 제거 (멘션 직후에 나오는 경우)
                            cleanText = cleanText?.replace(Regex("""^\s*さん、?\s*"""), "") ?: ""
                            
                            // 멘션들을 첫 줄에, 그 다음 줄부터 메시지
                            finalText = mentions.joinToString("\n") + "\n" + cleanText
                        }
                    }
                    
                    // 채널 전체 멘션 처리 (이미 멘션이 있지 않을 때만)
                    if (mentionAll && !hasDoorayMention) {
                        // 조직 ID를 얻기 위해 채널 정보 조회 (간단하게 하드코딩으로 처리 - 실제로는 채널 정보에서 가져와야 함)
                        val channelMention = "[@Channel](dooray://1708537451674140147/channels/$channelId \"channel\")\n"
                        finalText = channelMention + finalText
                    }
                    
                    // 개별 멤버 멘션 처리 (이미 멘션이 있지 않을 때만)
                    if (mentionMembers != null && !hasDoorayMention) {
                        val mentions = mutableListOf<String>()
                        mentionMembers.forEach { memberElement ->
                            val memberObj = memberElement.jsonObject
                            val memberId = memberObj["id"]?.jsonPrimitive?.content
                            val memberName = memberObj["name"]?.jsonPrimitive?.content
                            val organizationId = memberObj["organizationId"]?.jsonPrimitive?.content
                            
                            if (memberId != null && memberName != null && organizationId != null) {
                                val mention = "[@$memberName](dooray://$organizationId/members/$memberId \"member\")"
                                mentions.add(mention)
                            }
                        }
                        
                        if (mentions.isNotEmpty()) {
                            // 멘션과 메시지를 줄바꿈으로 분리하여 더 자연스러운 형식으로 만듦
                            finalText = mentions.joinToString("\n") + "\n" + finalText
                        }
                    }

                    val sendMessageRequest = SendChannelMessageRequest(
                        text = finalText,
                        messageType = messageType
                    )

                    val response = doorayClient.sendChannelMessage(channelId, sendMessageRequest)

                    if (response.header.isSuccessful) {
                        val data = ChannelMessageResponseData(
                            channelId = channelId,
                            sentText = finalText,
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