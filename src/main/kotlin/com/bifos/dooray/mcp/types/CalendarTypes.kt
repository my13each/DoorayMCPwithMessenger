package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

// ============ 캘린더 목록 조회 ============

@Serializable
data class CalendarListResponse(
    val header: DoorayApiHeader,
    val result: List<Calendar>,
    val totalCount: Int
)

@Serializable
data class Calendar(
    val id: String,
    val name: String,
    val type: String, // private, project, subscription
    val createdAt: String? = null,
    val ownerOrganizationMemberId: String? = null,
    val projectId: String? = null, // project type인 경우
    val me: CalendarMe
)

@Serializable
data class CalendarMe(
    val default: Boolean,
    val color: String,
    val listed: Boolean,
    val checked: Boolean,
    val role: String, // owner, delegatee, all, read_write, view, opaque_view
    val order: Int
)

// ============ 캘린더 이벤트 목록 조회 ============

@Serializable
data class CalendarEventsResponse(
    val header: DoorayApiHeader,
    val result: List<CalendarEvent>
)

@Serializable
data class CalendarEvent(
    val id: String,
    val masterScheduleId: String,
    val calendar: EventCalendar,
    val project: EventProject? = null, // 프로젝트 캘린더인 경우
    val recurrenceId: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val dueDate: String? = null, // 업무인 경우
    val dueDateClass: String? = null, // morning, lunch, evening
    val location: String? = null,
    val subject: String,
    val createdAt: String,
    val updatedAt: String,
    val category: String, // general, post, milestone
    val wholeDayFlag: Boolean = false,
    val tenant: EventTenant,
    val uid: String,
    val recurrenceType: String, // none, modified, unmodified
    val conferencing: EventConferencing? = null,
    val me: EventMe
)

@Serializable
data class EventCalendar(
    val id: String,
    val name: String
)

@Serializable
data class EventProject(
    val id: String,
    val name: String
)

@Serializable
data class EventTenant(
    val id: String
)

@Serializable
data class EventConferencing(
    val key: String,
    val serviceType: String,
    val url: String
)

@Serializable
data class EventMe(
    val type: String, // member
    val member: EventMemberInfo? = null,
    val status: String? = null, // accepted, declined, tentative, not_confirmed
    val userType: String? = null // from, to, cc
)

@Serializable
data class EventMemberInfo(
    val organizationMemberId: String,
    val emailAddress: String? = null,
    val name: String? = null
)

// ============ 캘린더 이벤트 생성 ============

@Serializable
data class CreateCalendarEventRequest(
    val users: EventUsers,
    val subject: String,
    val body: EventBody,
    val startedAt: String,
    val endedAt: String,
    val wholeDayFlag: Boolean = false,
    val location: String? = null,
    val recurrenceRule: RecurrenceRule? = null,
    val personalSettings: PersonalSettings? = null
)

@Serializable
data class EventUsers(
    val to: List<EventUser>,
    val cc: List<EventUser> = emptyList()
)

@Serializable
data class EventUser(
    val type: String, // member, emailUser
    val member: EventUserMember? = null,
    val emailUser: EventUserEmail? = null
)

@Serializable
data class EventUserMember(
    val organizationMemberId: String
)

@Serializable
data class EventUserEmail(
    val emailAddress: String,
    val name: String? = null
)

@Serializable
data class EventBody(
    val mimeType: String = "text/html",
    val content: String
)

@Serializable
data class RecurrenceRule(
    val frequency: String, // daily, weekly, monthly, yearly
    val interval: Int = 1,
    val until: String? = null,
    val byday: String? = null, // SU, MO, TU, WE, TH, FR, ST, 1 MO, 2 TU, -1 WE, -2 TH etc.
    val bymonth: String? = null, // 1 - 12
    val bymonthday: String? = null, // 1 - 31
    val timezoneName: String = "Asia/Seoul"
)

@Serializable
data class PersonalSettings(
    val alarms: List<EventAlarm> = emptyList(),
    val busy: Boolean = true, // true: 예정있음, false: 공백시간표시
    val `class`: String = "public" // public: 공개, private: 비공개
)

@Serializable
data class EventAlarm(
    val action: String, // mail, app
    val trigger: String // rfc2445, duration, trigger
)

@Serializable
data class CalendarEventCreateResponse(
    val header: DoorayApiHeader,
    val result: CreatedEvent
)

@Serializable
data class CreatedEvent(
    val id: String,
    val tenant: EventTenant? = null,
    val calendar: EventCalendar? = null,
    val uid: String? = null,
    val masterScheduleId: String? = null,
    val recurrenceId: String? = null,
    val recurrenceType: String? = null,
    val wholeDayFlag: Boolean? = null,
    val category: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val subject: String? = null,
    val location: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val files: String? = null
)