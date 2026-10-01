package com.my13each.dooray.mcp.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ============ 공통 ============

/** resultMessage가 null이거나 result가 비어 있을 수 있는 응답 */
@Serializable
data class LenientApiResponse<T>(
    val header: LenientApiHeader,
    val result: T? = null
)

/** 생성 API의 id 결과 */
@Serializable
data class IdResult(val id: String)

/** result가 없는(null) 성공 응답 */
typealias LenientUnitResponse = LenientApiResponse<JsonElement>

// ============ 위키 페이지 첨부 ============

/** 위키 페이지 첨부 파일 / 인라인 이미지 */
@Serializable
data class WikiPageFile(
    val id: String,
    val name: String? = null,
    val size: Long? = null,
    val attachFileId: String? = null
)

// ============ 위키 페이지 관리 ============

/** 위키 페이지 이동 요청 */
@Serializable
data class MoveWikiPageRequest(
    val targetParentPageId: String,
    val targetWikiId: String? = null,
    val withChildren: Boolean? = null,
    val beforePageId: String? = null
)

/** 위키 페이지 제목 수정 요청 */
@Serializable
data class UpdateWikiPageTitleRequest(val subject: String)

/** 위키 페이지 본문 수정 요청 */
@Serializable
data class UpdateWikiPageContentRequest(val body: WikiPageBody)

/** 위키 페이지 참조자 수정 요청 */
@Serializable
data class UpdateWikiPageReferrersRequest(val referrers: List<WikiReferrer>)

// ============ 위키 댓글 ============

/** 위키 댓글 본문 */
@Serializable
data class WikiCommentBody(
    val content: String,
    val mimeType: String? = null
)

/** 위키 댓글 생성/수정 요청 */
@Serializable
data class WikiCommentRequest(val body: WikiCommentBody)

/** 위키 댓글 작성자 */
@Serializable
data class WikiCommentCreator(
    val type: String? = null,
    val member: Member? = null
)

/** 위키 댓글이 달린 페이지 */
@Serializable
data class WikiCommentPage(val id: String)

/** 위키 댓글 */
@Serializable
data class WikiComment(
    val id: String,
    val page: WikiCommentPage? = null,
    val createdAt: String? = null,
    val modifiedAt: String? = null,
    val creator: WikiCommentCreator? = null,
    val body: WikiCommentBody? = null
)

/** 위키 댓글 목록 응답 */
@Serializable
data class WikiCommentListResponse(
    val header: LenientApiHeader,
    val result: List<WikiComment> = emptyList(),
    val totalCount: Int? = null
)

// ============ 위키 파일 업로드 ============

/** 위키 파일 업로드 결과 */
@Serializable
data class WikiUploadedFile(
    val id: String,
    val attachFileId: String? = null,
    val name: String? = null,
    val mimeType: String? = null,
    val type: String? = null,
    val size: Long? = null,
    val createdAt: String? = null
)
