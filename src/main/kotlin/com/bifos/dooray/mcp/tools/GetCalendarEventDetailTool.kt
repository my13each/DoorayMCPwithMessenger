package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.*
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun getCalendarEventDetailTool(): Tool {
    return Tool(
        name = "dooray_calendar_event_detail",
        description = "두레이 캘린더에서 특정 일정의 상세 정보를 조회합니다. 전체 참석자 정보, 회의 내용, 첨부파일 등을 확인할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("calendarId") {
                    put("type", "string")
                    put("description", "캘린더 ID (dooray_calendar_list 또는 dooray_calendar_events에서 확인 가능)")
                }
                putJsonObject("eventId") {
                    put("type", "string")
                    put("description", "일정 ID (dooray_calendar_events에서 확인 가능)")
                }
            },
            required = listOf("calendarId", "eventId")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getCalendarEventDetailHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val calendarId = request.arguments["calendarId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("calendarId is required")
            val eventId = request.arguments["eventId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("eventId is required")

            val response = doorayClient.getCalendarEventDetail(calendarId, eventId)
            
            if (response.header.isSuccessful) {
                val event = response.result
                
                // 참석자 정보 포맷팅
                val participantsInfo = buildString {
                    event.users?.let { users ->
                        // 주최자
                        users.from?.let { organizer ->
                            appendLine("👑 **주최자:**")
                            when (organizer.type) {
                                "member" -> {
                                    organizer.member?.let { member ->
                                        appendLine("- ${member.name ?: "이름 없음"} (${member.emailAddress ?: member.organizationMemberId})")
                                        organizer.status?.let { status ->
                                            appendLine("  상태: $status")
                                        }
                                    }
                                }
                                "emailUser" -> {
                                    organizer.emailUser?.let { email ->
                                        appendLine("- ${email.name ?: "이름 없음"} (${email.emailAddress})")
                                    }
                                }
                            }
                            appendLine()
                        }
                        
                        // 참석자
                        if (users.to.isNotEmpty()) {
                            appendLine("✅ **참석자 (${users.to.size}명):**")
                            users.to.forEach { participant ->
                                when (participant.type) {
                                    "member" -> {
                                        participant.member?.let { member ->
                                            val statusEmoji = when (participant.status) {
                                                "accepted" -> "✅"
                                                "declined" -> "❌"
                                                "tentative" -> "❓"
                                                "not_confirmed" -> "⏳"
                                                else -> "❔"
                                            }
                                            appendLine("- $statusEmoji ${member.name ?: "이름 없음"} (${member.emailAddress ?: member.organizationMemberId}) - ${participant.status ?: "확인 중"}")
                                        }
                                    }
                                    "emailUser" -> {
                                        participant.emailUser?.let { email ->
                                            val statusEmoji = when (participant.status) {
                                                "accepted" -> "✅"
                                                "declined" -> "❌"
                                                "tentative" -> "❓"
                                                "not_confirmed" -> "⏳"
                                                else -> "❔"
                                            }
                                            appendLine("- $statusEmoji ${email.name ?: "이름 없음"} (${email.emailAddress}) - ${participant.status ?: "확인 중"}")
                                        }
                                    }
                                }
                            }
                            appendLine()
                        }
                        
                        // 참조자
                        if (users.cc.isNotEmpty()) {
                            appendLine("📋 **참조자 (${users.cc.size}명):**")
                            users.cc.forEach { cc ->
                                when (cc.type) {
                                    "member" -> {
                                        cc.member?.let { member ->
                                            appendLine("- ${member.name ?: "이름 없음"} (${member.emailAddress ?: member.organizationMemberId})")
                                        }
                                    }
                                    "emailUser" -> {
                                        cc.emailUser?.let { email ->
                                            appendLine("- ${email.name ?: "이름 없음"} (${email.emailAddress})")
                                        }
                                    }
                                }
                            }
                        }
                    } ?: appendLine("참석자 정보가 없습니다.")
                }
                
                val successData = CalendarEventDetailResponseData(
                    id = event.id,
                    subject = event.subject,
                    calendar = event.calendar.name,
                    startedAt = (event.startedAt ?: "시간 정보 없음"),
                    endedAt = (event.endedAt ?: "시간 정보 없음"),
                    location = (event.location ?: "장소 정보 없음"),
                    category = event.category,
                    wholeDayFlag = event.wholeDayFlag,
                    organizer = (event.users?.from?.member?.name ?: event.users?.from?.emailUser?.name ?: "정보 없음"),
                    participantCount = (event.users?.to?.size ?: 0),
                    ccCount = (event.users?.cc?.size ?: 0),
                    body = (event.body?.content?.take(200) ?: "내용 없음"),
                    fileCount = (event.files?.size ?: 0),
                    recurrenceType = event.recurrenceType
                )
                
                val successResponse = ToolSuccessResponse(
                    data = successData,
                    message = "📅 **${event.subject}** 일정 상세 정보\n\n" +
                            "🗓️ **일시:** ${event.startedAt ?: "시간 정보 없음"} ~ ${event.endedAt ?: "시간 정보 없음"}\n" +
                            "📍 **장소:** ${event.location ?: "장소 정보 없음"}\n" +
                            "📝 **카테고리:** ${event.category}\n" +
                            "${if (event.wholeDayFlag) "🌅 종일 일정\n" else ""}" +
                            "📋 **캘린더:** ${event.calendar.name}\n\n" +
                            participantsInfo +
                            "\n📄 **회의 내용:**\n${event.body?.content?.take(300) ?: "내용이 없습니다."}" +
                            "${if ((event.body?.content?.length ?: 0) > 300) "..." else ""}\n" +
                            "${if ((event.files?.size ?: 0) > 0) "\n📎 첨부파일: ${event.files?.size}개" else ""}"
                )
                
                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = response.header.resultMessage,
                    code = "DOORAY_API_${response.header.resultCode}"
                ).toErrorResponse()
                
                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            }
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "캘린더 일정 상세 조회 중 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}