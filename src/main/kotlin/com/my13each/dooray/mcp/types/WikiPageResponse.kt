package com.my13each.dooray.mcp.types

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 위키 페이지의 본문 정보 */
@Serializable
data class WikiPageBody(val mimeType: String, val content: String? = null)

/** 위키 페이지 상세 정보 */
@Serializable
data class WikiPageDetail(
    val id: String,
    val wikiId: String,
    val version: Int,
    val root: Boolean,
    val creator: Creator,
    val subject: String,
    val body: WikiPageBody? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val parentPageId: String? = null,
    /** 참조자 목록 */
    val referrers: List<WikiReferrer>? = null,
    /** 첨부 파일 목록 (id는 페이지 파일 다운로드/삭제에 사용) */
    val files: List<WikiPageFile>? = null,
    /** 본문 인라인 이미지 목록 */
    val images: List<WikiPageFile>? = null
)

/** 위키 페이지 생성 결과 (축약된 정보) */
@Serializable
data class CreateWikiPageResult(
    val id: String,
    val wikiId: String,
    val parentPageId: String? = null,
    val version: Int
)

/** 위키 페이지 생성 요청 */
@Serializable
data class CreateWikiPageRequest(
    val subject: String,
    val body: WikiPageBody,
    val parentPageId: String, // 필수 필드
    val attachFileIds: List<String>? = null,
    val referrers: List<WikiReferrer>? = null
)

/** 위키 페이지 수정 요청 */
@Serializable
data class UpdateWikiPageRequest(
    val subject: String? = null,
    val body: WikiPageBody? = null,
    val referrers: List<WikiReferrer>? = null
)

/**
 * 위키 참조자 정보.
 * type은 API 필수값이지만 기본값이라 encodeDefaults=false인 HTTP 클라이언트에서 빠지므로 @EncodeDefault로 항상 직렬화한다.
 * (type이 빠지면 400 "入力した内容が正しくありません")
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class WikiReferrer(@EncodeDefault val type: String = "member", val member: Member)

/** 위키 페이지 상세 응답 타입 */
typealias WikiPageResponse = DoorayApiResponse<WikiPageDetail>

/** 위키 페이지 생성 응답 타입 */
typealias CreateWikiPageResponse = DoorayApiResponse<CreateWikiPageResult>
