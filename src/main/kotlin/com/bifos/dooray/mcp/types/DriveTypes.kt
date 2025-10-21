package com.bifos.dooray.mcp.types

import kotlinx.serialization.Serializable

// ============ Drive 관련 타입들 ============

/** Drive 정보 */
@Serializable
data class Drive(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val organizationId: String? = null
)

/** Drive 목록 응답 */
@Serializable
data class DriveListResponse(
    val header: DoorayApiHeader,
    val result: List<Drive>,
    val totalCount: Int? = null
)

/** Drive 상세 응답 */
@Serializable
data class DriveDetailResponse(
    val header: DoorayApiHeader,
    val result: Drive?
)

// ============ Drive File 관련 타입들 ============

/** Drive 파일 정보 */
@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String? = null,
    val size: Long? = null,
    val parentId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val downloadUrl: String? = null,
    val webViewUrl: String? = null,
    val thumbnailUrl: String? = null,
    val isFolder: Boolean? = false,
    val createdBy: String? = null,
    val modifiedBy: String? = null
)

/** Drive 파일 목록 응답 */
@Serializable
data class DriveFileListResponse(
    val header: DoorayApiHeader,
    val result: List<DriveFile>,
    val totalCount: Int? = null
)

/** Drive 파일 상세 응답 */
@Serializable
data class DriveFileDetailResponse(
    val header: DoorayApiHeader,
    val result: DriveFile?
)

/** 파일 업로드 요청 */
@Serializable
data class UploadFileRequest(
    val name: String,
    val parentId: String? = null,
    val content: String, // Base64 encoded file content
    val mimeType: String? = null
)

/** 파일 업로드 결과 */
@Serializable
data class UploadFileResult(
    val id: String,
    val name: String,
    val downloadUrl: String? = null,
    val webViewUrl: String? = null
)

/** 파일 업로드 응답 */
@Serializable
data class UploadFileResponse(
    val header: DoorayApiHeader,
    val result: UploadFileResult?
)

/** 폴더 생성 요청 */
@Serializable
data class CreateFolderRequest(
    val name: String
)

/** 폴더 생성 결과 */
@Serializable
data class CreateFolderResult(
    val id: String,
    val name: String
)

/** 폴더 생성 응답 */
@Serializable
data class CreateFolderResponse(
    val header: DoorayApiHeader,
    val result: CreateFolderResult?
)

/** 파일 복사/이동 요청 */
@Serializable
data class CopyMoveFileRequest(
    val parentId: String,
    val name: String? = null
)

/** 파일 복사/이동 결과 */
@Serializable
data class CopyMoveFileResult(
    val id: String,
    val name: String,
    val parentId: String
)

/** 파일 복사/이동 응답 */
@Serializable
data class CopyMoveFileResponse(
    val header: DoorayApiHeader,
    val result: CopyMoveFileResult?
)

/** 파일 수정 요청 */
@Serializable
data class UpdateFileRequest(
    val name: String? = null,
    val content: String? = null, // Base64 encoded content for updating file content
    val mimeType: String? = null
)

/** 파일 삭제 응답 */
typealias DeleteFileResponse = DoorayApiUnitResponse

// ============ 도구 응답용 데이터 타입들 ============

/** Drive 목록 응답 데이터 (도구용) */
@Serializable
data class DriveListResponseData(
    val drives: List<Drive>,
    val totalCount: Int
)

/** Drive 파일 목록 응답 데이터 (도구용) */
@Serializable
data class DriveFileListResponseData(
    val files: List<DriveFile>,
    val totalCount: Int,
    val driveId: String
)