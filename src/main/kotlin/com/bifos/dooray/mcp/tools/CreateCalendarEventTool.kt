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

fun createCalendarEventTool(): Tool {
    return Tool(
        name = "dooray_calendar_create_event",
        description = "두레이 캘린더에 새로운 일정을 등록합니다. 회의, 약속 등의 일정을 생성할 때 사용하세요.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("calendarId") {
                    put("type", "string")
                    put("description", "일정을 등록할 캘린더 ID (dooray_calendar_list에서 확인 가능)")
                }
                putJsonObject("subject") {
                    put("type", "string")
                    put("description", "일정 제목")
                }
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "일정 내용/설명")
                }
                putJsonObject("toMemberIds") {
                    put("type", "string")
                    put("description", "참석자 멤버 ID들 (쉼표로 구분, 예: member1,member2)")
                }
                putJsonObject("ccMemberIds") {
                    put("type", "string")
                    put("description", "참조자 멤버 ID들 (쉼표로 구분, 선택사항)")
                }
                putJsonObject("startedAt") {
                    put("type", "string")
                    put("description", "일정 시작 시간 (ISO 8601 형식, 예: 2025-04-11T09:00:00+09:00)")
                }
                putJsonObject("endedAt") {
                    put("type", "string")
                    put("description", "일정 종료 시간 (ISO 8601 형식, 예: 2025-04-11T11:00:00+09:00)")
                }
                putJsonObject("wholeDayFlag") {
                    put("type", "boolean")
                    put("description", "종일 일정 여부 (기본값: false)")
                    put("default", false)
                }
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "일정 장소 (선택사항)")
                }
            },
            required = listOf("calendarId", "subject", "content", "startedAt", "endedAt", "toMemberIds")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun createCalendarEventHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val calendarId = request.arguments["calendarId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("calendarId is required")
            val subject = request.arguments["subject"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("subject is required")
            val content = request.arguments["content"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("content is required")
            val startedAt = request.arguments["startedAt"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("startedAt is required")
            val endedAt = request.arguments["endedAt"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("endedAt is required")
            val toMemberIds = request.arguments["toMemberIds"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("toMemberIds is required")
            val ccMemberIds = request.arguments["ccMemberIds"]?.jsonPrimitive?.content
            val wholeDayFlag = request.arguments["wholeDayFlag"]?.jsonPrimitive?.boolean ?: false
            val location = request.arguments["location"]?.jsonPrimitive?.content
            
            // 참석자 목록 구성
            val toUsers = toMemberIds.split(",").map { memberId ->
                EventUser(
                    type = "member",
                    member = EventUserMember(organizationMemberId = memberId.trim())
                )
            }
            
            val ccUsers = if (ccMemberIds.isNullOrBlank()) {
                emptyList()
            } else {
                ccMemberIds.split(",").map { memberId ->
                    EventUser(
                        type = "member", 
                        member = EventUserMember(organizationMemberId = memberId.trim())
                    )
                }
            }
                
            val requestBody = CreateCalendarEventRequest(
                users = EventUsers(
                    to = toUsers,
                    cc = ccUsers
                ),
                subject = subject,
                body = EventBody(
                    mimeType = "text/html",
                    content = content
                ),
                startedAt = startedAt,
                endedAt = endedAt,
                wholeDayFlag = wholeDayFlag,
                location = location
            )

            val response = doorayClient.createCalendarEvent(calendarId, requestBody)
            
            if (response.header.isSuccessful) {
                val successData = mapOf(
                    "eventId" to response.result.id,
                    "calendarId" to calendarId,
                    "subject" to subject,
                    "startedAt" to startedAt,
                    "endedAt" to endedAt
                )
                
                val successResponse = ToolSuccessResponse(
                    data = successData,
                    message = "📅 캘린더 일정이 성공적으로 등록되었습니다!\n\n💡 등록된 일정: $subject\n🕒 시간: $startedAt ~ $endedAt"
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
                message = "캘린더 일정 등록 중 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}