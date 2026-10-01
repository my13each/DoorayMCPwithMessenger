package com.my13each.dooray.mcp.types

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
        val count: Int,
        val requestedSize: Int
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

/** 스레드 생성 응답 데이터 */
@Serializable
data class CreateThreadResponseData(
        val channelId: String,
        /** 스레드 채널 ID (이 ID로 send_channel_message 하면 같은 스레드에 이어서 전송) */
        val threadId: String?,
        val logId: String,
        val sentText: String,
        val timestamp: Long
)

/** 메시지 수정 응답 데이터 */
@Serializable
data class UpdateMessageResponseData(
        val channelId: String,
        val logId: String,
        val updatedText: String,
        val timestamp: Long
)

/** 메시지 삭제 응답 데이터 */
@Serializable
data class DeleteMessageResponseData(
        val channelId: String,
        val logId: String,
        val timestamp: Long
)

/** 답장 · 기존 메시지 스레드 전송 응답 데이터 */
@Serializable
data class MessageSendResponseData(
        val channelId: String,
        val targetLogId: String,
        val logId: String?,
        val sentChannelId: String?,
        val sentText: String
)

/** 메신저 첨부 파일 다운로드 응답 데이터 */
@Serializable
data class MessengerFileDownloadResponseData(
        val channelId: String,
        val fileId: String,
        val fileName: String,
        val savedPath: String,
        /** Docker 실행 시 호스트에서의 경로 힌트 (예: ~/Downloads/a.pdf) */
        val hostPathHint: String?,
        val size: Long,
        val contentType: String?,
        /** 텍스트 파일(50KB 이하)인 경우 내용 */
        val textContent: String?
)

/** 채널 가입 응답 데이터 */
@Serializable
data class JoinChannelResponseData(
        val channelId: String,
        val memberIds: List<String>,
        val timestamp: Long
)

/** 채널 탈퇴 응답 데이터 */
@Serializable
data class LeaveChannelResponseData(
        val channelId: String,
        val memberIds: List<String>,
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
