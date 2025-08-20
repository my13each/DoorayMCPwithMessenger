package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CalendarListResponse
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject

fun getCalendarsTool(): Tool {
    return Tool(
        name = "dooray_calendar_list",
        description = "두레이에서 접근 가능한 캘린더 목록을 조회합니다. 캘린더 ID를 찾거나 사용 가능한 캘린더를 확인할 때 사용하세요.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                // 캘린더 목록 조회는 별도 파라미터가 필요하지 않음
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getCalendarsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val response = doorayClient.getCalendars()
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "📅 캘린더 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)\n\n💡 다음 단계: 특정 기간의 일정을 보려면 dooray_calendar_events를 사용하세요."
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
                message = "캘린더 목록 조회 중 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}