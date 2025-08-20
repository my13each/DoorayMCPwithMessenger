package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

@Serializable
data class CalendarListResponse(
    val header: DoorayApiHeader,
    val result: List<Calendar>
)

@Serializable
data class Calendar(
    val id: String,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    val type: String? = null
)

@Serializable
data class CalendarEventsResponse(
    val header: DoorayApiHeader,
    val result: List<CalendarEvent>
)

@Serializable
data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val subject: String,
    val content: String? = null,
    val startedAt: String,
    val endedAt: String,
    val wholeDayFlag: Boolean = false,
    val location: String? = null,
    val creator: EventCreator? = null
)

@Serializable
data class EventCreator(
    val type: String,
    val member: EventMember? = null
)

@Serializable
data class EventMember(
    val organizationMemberId: String
)

@Serializable
data class CreateCalendarEventRequest(
    val subject: String,
    val body: EventBody,
    val startedAt: String,
    val endedAt: String,
    val wholeDayFlag: Boolean = false,
    val location: String? = null
)

@Serializable
data class EventBody(
    val mimeType: String = "text/html",
    val content: String
)

@Serializable
data class CalendarEventCreateResponse(
    val header: DoorayApiHeader,
    val result: CreatedEvent
)

@Serializable
data class CreatedEvent(
    val id: String,
    val calendarId: String
)