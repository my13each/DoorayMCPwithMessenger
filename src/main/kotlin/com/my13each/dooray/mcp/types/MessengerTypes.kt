package com.my13each.dooray.mcp.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
    val role: String? = null // "member", "creator", "admin" - 옵셔널로 수정
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

/** 채널 메시지 발신자 멤버 */
@Serializable
data class ChannelMessageSenderMember(
    val organizationMemberId: String? = null
)

/** 채널 메시지 발신자 (type: member, app 등) */
@Serializable
data class ChannelMessageSender(
    val type: String? = null,
    val member: ChannelMessageSenderMember? = null,
    /** 봇(앱) 발신자 정보 (type=app일 때, appId 포함) */
    val app: JsonObject? = null
)

/** 채널 메시지 (GET /messenger/v1/channels/{channel-id}/logs 응답 항목) */
@Serializable
data class ChannelMessage(
    val id: String,
    val seq: Long? = null,
    val type: String? = null,
    val sender: ChannelMessageSender? = null,
    val sentAt: String? = null,
    val text: String? = null,
    val flags: String? = null,
    /** 번역 등 부가 정보 (type별 구조가 달라 원본 유지) */
    val attachments: List<JsonObject>? = null,
    /** 첨부 파일 정보 (file.id로 파일 다운로드 가능) */
    val file: JsonObject? = null,
    /** 발신자 이름 (도구에서 멤버 조회로 채움, API 원본에는 없음) */
    val senderName: String? = null,
    /** REPLY 메시지의 원본 text (JSON 문자열). text에는 답장 본문만 풀어서 넣음 */
    val rawText: String? = null
)

/** resultMessage가 null로 올 수 있는 메신저 API용 응답 헤더 */
@Serializable
data class LenientApiHeader(
    val isSuccessful: Boolean,
    val resultCode: Int,
    val resultMessage: String? = null
)

/** 채널 로그 조회 응답 */
@Serializable
data class ChannelLogsResponse(
    val header: LenientApiHeader,
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

// ============ 스레드 관련 타입들 ============

/** 스레드 생성 및 메시지 전송 요청 */
@Serializable
data class CreateThreadRequest(
    /** 대화방에 보낼 메시지 */
    val text: String,
    /** 글타래(스레드)에 보낼 첫 메시지 (생략 시 Dooray가 "#"을 넣음) */
    val threadText: String? = null,
    val messageType: String? = "text"
)

/** 스레드 생성 결과 (id: 스레드 채널의 log-id, channelId: 스레드 채널 ID) */
@Serializable
data class CreateThreadResult(
    val id: String,
    val channelId: String? = null
)

/** 스레드 생성 응답 */
@Serializable
data class CreateThreadResponse(
    val header: LenientApiHeader,
    val result: CreateThreadResult? = null
)

// ============ 답장 / 기존 메시지 스레드 관련 타입들 ============

/** 메시지 답장 · 기존 메시지 스레드 전송 요청 */
@Serializable
data class MessageTextRequest(
    val text: String
)

/** 메시지 전송 결과 (log-id + 전송된 채널 ID) */
@Serializable
data class MessageSendResult(
    val id: String,
    val channelId: String? = null
)

/** 메시지 답장 · 기존 메시지 스레드 전송 응답 */
@Serializable
data class MessageSendResponse(
    val header: LenientApiHeader,
    val result: MessageSendResult? = null
)

// ============ 멤버 상세 관련 타입들 ============

/** 멤버 상세 정보 (GET /common/v1/members/{member-id}) */
@Serializable
data class MemberDetail(
    val id: String,
    val name: String? = null,
    val englishName: String? = null,
    val nativeName: String? = null,
    val nickname: String? = null,
    val userCode: String? = null,
    val externalEmailAddress: String? = null,
    val idProviderType: String? = null,
    val idProviderUserId: String? = null,
    val locale: String? = null,
    val timezoneName: String? = null
)

/** 멤버 상세 조회 응답 */
@Serializable
data class MemberDetailResponse(
    val header: LenientApiHeader,
    val result: MemberDetail? = null
)

// ============ 메신저 파일 관련 타입들 ============

/** 메신저 첨부 파일 다운로드 결과 (바이너리) */
class MessengerFileDownload(
    val bytes: ByteArray,
    val fileName: String?,
    val contentType: String?
)

// ============ 메시지 수정/삭제 관련 타입들 ============

/** 메시지 수정 요청 */
@Serializable
data class UpdateMessageRequest(
    val text: String,
    val messageType: String? = "text"
)

/** 메시지 수정 응답 */
typealias UpdateMessageResponse = DoorayApiUnitResponse

/** 메시지 삭제 응답 */
typealias DeleteMessageResponse = DoorayApiUnitResponse

// ============ 도구 응답용 데이터 타입들 ============

/** 간단한 채널 목록 응답 데이터 (도구용) */
@Serializable
data class SimpleChannelListResponseData(
    val channels: List<SimpleChannel>,
    val totalCount: Int
)