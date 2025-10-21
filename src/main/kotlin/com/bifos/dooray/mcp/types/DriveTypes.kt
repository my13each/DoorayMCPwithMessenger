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

/** 파일 업로드 요청 - multipart/form-data용 */
data class UploadFileRequest(
    val fileName: String,
    val fileContent: ByteArray,
    val parentId: String,
    val mimeType: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadFileRequest
        return fileName == other.fileName && 
               fileContent.contentEquals(other.fileContent) &&
               parentId == other.parentId &&
               mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        result = 31 * result + parentId.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}

/** Base64 파일 업로드 요청 (MCP 도구용) */
@Serializable  
data class Base64UploadRequest(
    val fileName: String,
    val base64Content: String,
    val parentId: String,
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

/** 파일 메타정보 */
@Serializable
data class DriveFileMetadata(
    val id: String,
    val driveId: String,
    val name: String,
    val version: Int? = null,
    val revision: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val creator: OrganizationMember? = null,
    val lastUpdater: OrganizationMember? = null,
    val type: String, // "file" or "folder"
    val hasFolders: Boolean? = null, // folder인 경우에만 의미가 있음
    val subType: String? = null, // folder(root, trash, users), file(etc, doc, photo, movie, music, zip)
    val mimeType: String? = null,
    val size: Long? = null, // folder인 경우 null
    val annotations: FileAnnotations? = null,
    val parentFile: ParentFileInfo? = null
)

/** 파일 메타정보 응답 */
@Serializable
data class DriveFileMetadataResponse(
    val header: DoorayApiHeader,
    val result: DriveFileMetadata?
)

/** 파일 애너테이션 정보 */
@Serializable
data class FileAnnotations(
    val favorited: Boolean? = null,
    val favoritedAt: String? = null
)

/** 부모 파일 정보 */
@Serializable
data class ParentFileInfo(
    val id: String,
    val path: String? = null // parent의 full-path
)

/** 파일 업데이트 결과 */
@Serializable
data class UpdateFileResult(
    val id: String,
    val version: Int? = null
)

/** 파일 업데이트 응답 */
@Serializable
data class UpdateFileResponse(
    val header: DoorayApiHeader,
    val result: UpdateFileResult?
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