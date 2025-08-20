package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CalendarEventsResponse
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun getCalendarEventsTool(): Tool {
    return Tool(
        name = "dooray_calendar_events",
        description = "두레이 캘린더에서 지정된 기간의 일정 목록을 조회합니다. 특정 날짜나 기간의 일정을 확인할 때 사용하세요.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("calendars") {
                    put("type", "string")
                    put("description", "조회할 캘린더 ID들 (쉼표로 구분, 비워두면 모든 캘린더)")
                }
                putJsonObject("timeMin") {
                    put("type", "string")
                    put("description", "조회 시작 시간 (ISO 8601 형식, 예: 2025-04-11T00:00:00+09:00)")
                }
                putJsonObject("timeMax") {
                    put("type", "string") 
                    put("description", "조회 종료 시간 (ISO 8601 형식, 예: 2025-04-12T00:00:00+09:00)")
                }
                putJsonObject("postType") {
                    put("type", "string")
                    put("description", "참석자 필터 (toMe: 나에게 온 일정, toCcMe: 나에게 온 일정+참조, fromToCcMe: 모든 관련 일정, 선택사항)")
                }
                putJsonObject("category") {
                    put("type", "string")
                    put("description", "카테고리 필터 (general: 일반 일정, post: 업무, milestone: 마일스톤, 선택사항)")
                }
            },
            required = listOf("timeMin", "timeMax")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getCalendarEventsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val calendars = request.arguments["calendars"]?.jsonPrimitive?.content
            val timeMin = request.arguments["timeMin"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("timeMin is required")
            val timeMax = request.arguments["timeMax"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("timeMax is required")
            val postType = request.arguments["postType"]?.jsonPrimitive?.content
            val category = request.arguments["category"]?.jsonPrimitive?.content
            
            val response = doorayClient.getCalendarEvents(calendars, timeMin, timeMax, postType, category)
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "📅 캘린더 일정을 성공적으로 조회했습니다 (총 ${response.result.size}개)\n\n💡 다음 단계: 새 일정을 등록하려면 dooray_calendar_create_event를 사용하세요."
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
                message = "캘린더 일정 조회 중 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}