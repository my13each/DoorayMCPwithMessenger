package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

// ============ 멤버 검색 관련 타입들 ============

/** 멤버 검색 요청 */
@Serializable
data class SearchMemberRequest(
    val name: String? = null,
    val externalEmailAddresses: List<String>? = null,
    val userCode: String? = null,
    val idProviderUserId: String? = null,
    val page: Int? = null,
    val size: Int? = null
)

/** 조직 멤버 정보 */
@Serializable
data class OrganizationMember(
    val id: String,
    val userCode: String,
    val name: String,
    val externalEmailAddress: String
)

/** 멤버 검색 응답 */
@Serializable
data class MemberSearchResponse(
    val header: DoorayApiHeader,
    val result: List<OrganizationMember>,
    val totalCount: Int
)

// ============ 다이렉트 메시지 관련 타입들 ============

/** 다이렉트 메시지 전송 요청 */
@Serializable
data class DirectMessageRequest(
    val text: String,
    val organizationMemberId: String
)

/** 다이렉트 메시지 전송 응답 */
typealias DirectMessageResponse = DoorayApiUnitResponse

// ============ 채널 관련 타입들 ============

/** 채널 참가자 멤버 정보 */
@Serializable
data class ChannelParticipantMember(
    val organizationMemberId: String
)

/** 채널 참가자 정보 */
@Serializable
data class ChannelParticipant(
    val type: String, // "member"
    val member: ChannelParticipantMember
)

/** 채널 사용자 정보 */
@Serializable
data class ChannelUsers(
    val participants: List<ChannelParticipant>
)

/** 채널에서의 본인 멤버 정보 */
@Serializable
data class ChannelMeMember(
    val organizationMemberId: String
)

/** 채널에서의 본인 정보 */
@Serializable
data class ChannelMe(
    val type: String, // "member"
    val member: ChannelMeMember,
    val role: String // "member", "creator", "admin"
)

/** 채널 조직 정보 */
@Serializable
data class ChannelOrganization(
    val id: String
)

/** 채널 정보 - API 스펙에 맞게 정확히 복원 */
@Serializable
data class Channel(
    val id: String,
    val title: String? = null,        // 채널 제목/이름 (API 스펙 및 실제 응답)
    val organization: ChannelOrganization? = null,
    val type: String? = null,         // "direct", "private", "me", "bot"
    val users: ChannelUsers? = null,
    val me: ChannelMe? = null,
    val capacity: Int? = null,        // 채널 참가 가능 인원수
    val status: String? = null,       // "system", "normal", "archived", "deleted"
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val displayed: Boolean? = null,   // 표시 여부
    val role: String? = null,         // "member", "creator", "admin"
    val archivedAt: String? = null
)

/** 간단한 채널 정보 (검색용) */
@Serializable
data class SimpleChannel(
    val id: String,
    val title: String? = null,
    val type: String? = null,         // "direct", "private", "me", "bot"
    val status: String? = null,       // "system", "normal", "archived", "deleted"
    val updatedAt: String? = null,
    val participantCount: Int? = null // participants 수만 표시
)

/** 간단한 채널 목록 응답 */
@Serializable
data class SimpleChannelListResponse(
    val header: DoorayApiHeader,
    val result: List<SimpleChannel>,
    val totalCount: Int? = null
)

/** 채널 목록 응답 */
@Serializable
data class ChannelListResponse(
    val header: DoorayApiHeader,
    val result: List<Channel>,
    val totalCount: Int? = null
)

/** 채널 생성 요청 */
@Serializable
data class CreateChannelRequest(
    val type: String, // "private" 또는 "direct"
    val capacity: String? = null, // 참가 가능 인원수 (문자열)
    val memberIds: List<String>? = null,
    val title: String? = null // 채널 제목
)

/** 채널 생성 결과 */
@Serializable
data class CreateChannelResult(
    val id: String
)

/** 채널 생성 응답 */
@Serializable
data class CreateChannelResponse(
    val header: DoorayApiHeader,
    val result: CreateChannelResult?
)

/** 채널 가입 요청 */
@Serializable
data class JoinChannelRequest(
    val memberIds: List<String>
)

/** 채널 가입 응답 */
typealias JoinChannelResponse = DoorayApiUnitResponse

/** 채널 탈퇴 요청 */
@Serializable
data class LeaveChannelRequest(
    val memberIds: List<String>
)

/** 채널 탈퇴 응답 */
typealias LeaveChannelResponse = DoorayApiUnitResponse

// ============ 채널 로그(메시지) 관련 타입들 ============

/** 채널 메시지 */
@Serializable
data class ChannelMessage(
    val id: String,
    val text: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val messageType: String? = null
)

/** 채널 로그 조회 응답 */
@Serializable
data class ChannelLogsResponse(
    val header: DoorayApiHeader,
    val result: List<ChannelMessage>,
    val totalCount: Int? = null
)

/** 채널 메시지 전송 요청 */
@Serializable
data class SendChannelMessageRequest(
    val text: String,
    val messageType: String? = "text"
)

/** 채널 메시지 전송 응답 */
typealias SendChannelMessageResponse = DoorayApiUnitResponse

// ============ 도구 응답용 데이터 타입들 ============

/** 간단한 채널 목록 응답 데이터 (도구용) */
@Serializable
data class SimpleChannelListResponseData(
    val channels: List<SimpleChannel>,
    val totalCount: Int
)