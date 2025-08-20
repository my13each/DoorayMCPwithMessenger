package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

/** MCP Tool 성공 응답 */
@Serializable
data class ToolSuccessResponse<T>(
        val success: Boolean = true,
        val data: T,
        val message: String? = null
)

/** 댓글 목록 조회 응답 데이터 */
@Serializable
data class PostCommentsResponseData(
        val comments: List<PostComment>,
        val totalCount: Int,
        val currentPage: Int,
        val pageSize: Int
)

/** 메신저 채널 목록 응답 데이터 */
@Serializable
data class ChannelListResponseData(
        val channels: List<Channel>,
        val totalCount: Int
)

/** 메신저 채널 로그 응답 데이터 */
@Serializable
data class ChannelLogsResponseData(
        val channelId: String,
        val messages: List<ChannelMessage>,
        val totalCount: Int,
        val currentPage: Int,
        val pageSize: Int
)

/** 멤버 검색 응답 데이터 */
@Serializable
data class MemberSearchResponseData(
        val members: List<OrganizationMember>,
        val totalCount: Int,
        val currentPage: Int,
        val pageSize: Int
)

/** 다이렉트 메시지 전송 응답 데이터 */
@Serializable
data class DirectMessageResponseData(
        val recipientId: String,
        val sentMessage: String,
        val timestamp: Long
)

/** 채널 메시지 전송 응답 데이터 */
@Serializable
data class ChannelMessageResponseData(
        val channelId: String,
        val sentText: String,
        val timestamp: Long
)

/** 채널 생성 응답 데이터 */
@Serializable
data class CreateChannelResponseData(
        val channelId: String,
        val channelTitle: String,
        val channelType: String,
        val memberCount: Int,
        val timestamp: Long
)

/** MCP Tool 에러 응답 */
@Serializable
data class ToolErrorResponse(
        val isError: Boolean = true,
        val error: ToolError,
        val content: ToolErrorContent,
)

@Serializable data class ToolErrorContent(val type: String = "text", val text: String)

/** Tool 에러 정보 */
@Serializable
data class ToolError(val type: String, val code: String? = null, val details: String? = null)

/** 캘린더 목록 조회 응답 */
@Serializable
data class GetCalendarsResponse(
    val success: Boolean,
    val message: String,
    val calendars: List<Calendar>
)

/** 캘린더 일정 조회 응답 */
@Serializable  
data class GetCalendarEventsResponse(
    val success: Boolean,
    val message: String,
    val events: List<CalendarEvent>
)

/** 캘린더 일정 등록 응답 */
@Serializable
data class CreateCalendarEventResponse(
    val success: Boolean,
    val message: String,
    val eventId: String?,
    val calendarId: String?
)

/** 캘린더 이벤트 상세 조회 응답 데이터 */
@Serializable  
data class CalendarEventDetailResponseData(
        val id: String,
        val subject: String,
        val calendar: String,
        val startedAt: String,
        val endedAt: String,
        val location: String,
        val category: String,
        val wholeDayFlag: Boolean,
        val organizer: String,
        val participantCount: Int,
        val ccCount: Int,
        val body: String,
        val fileCount: Int,
        val recurrenceType: String
)
