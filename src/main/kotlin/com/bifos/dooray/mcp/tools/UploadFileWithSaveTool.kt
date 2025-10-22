package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.Base64UploadRequest
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.Base64

fun uploadFileWithSaveTool(): Tool {
    return Tool(
        name = "dooray_drive_upload_file_with_save",
        description = """
            **[RECOMMENDED for Excel/generated files]** Claude가 생성한 파일(Excel 등)을 Dooray 드라이브에 업로드합니다.

            ✅ **이 툴을 사용해야 하는 경우**:
            - Claude가 방금 생성한 Excel, PDF, 이미지 등의 파일을 업로드할 때
            - Base64 데이터가 있고 파일 경로가 불확실할 때
            - 파일을 Downloads 폴더에도 저장하고 싶을 때

            🚀 **자동 처리**:
            1. Base64 데이터를 받아서 Downloads 폴더에 저장
            2. 저장된 파일을 Dooray 드라이브에 업로드
            3. 업로드 완료 후 임시 파일 자동 정리 (keep_local=true로 보관 가능)

            ⚠️ **제한사항**: 최대 100MB
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "ドライブID")
                }
                putJsonObject("file_name") {
                    put("type", "string")
                    put("description", "ファイル名（拡張子含む）")
                }
                putJsonObject("base64_content") {
                    put("type", "string")
                    put("description", "Base64エンコードされたファイル内容")
                }
                putJsonObject("parent_id") {
                    put("type", "string")
                    put("description", "親フォルダID（必須）")
                }
                putJsonObject("mime_type") {
                    put("type", "string")
                    put("description", "MIMEタイプ（例: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）省略可能")
                }
                putJsonObject("keep_local") {
                    put("type", "boolean")
                    put("description", "アップロード後もDownloadsフォルダにファイルを残すか（デフォルト: false）")
                }
            },
            required = listOf("drive_id", "file_name", "base64_content", "parent_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun uploadFileWithSaveHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        var tempFile: File? = null
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_idは必須パラメータです。",
                    code = "MISSING_DRIVE_ID"
                )

            val fileName = request.arguments["file_name"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "file_nameは必須パラメータです。",
                    code = "MISSING_FILE_NAME"
                )

            val base64Content = request.arguments["base64_content"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "base64_contentは必須パラメータです。",
                    code = "MISSING_BASE64_CONTENT"
                )

            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "parent_idは必須パラメータです。",
                    code = "MISSING_PARENT_ID"
                )

            val mimeType = request.arguments["mime_type"]?.jsonPrimitive?.content
            val keepLocal = request.arguments["keep_local"]?.jsonPrimitive?.content?.toBoolean() ?: false

            // Step 1: Base64デコード
            val fileBytes = try {
                Base64.getDecoder().decode(base64Content)
            } catch (e: Exception) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "Base64デコードに失敗しました: ${e.message}",
                    code = "INVALID_BASE64"
                )
            }

            // ファイルサイズチェック（100MB制限）
            val maxSize = 100 * 1024 * 1024 // 100MB
            if (fileBytes.size > maxSize) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "ファイルサイズが大きすぎます（最大100MB）: ${fileBytes.size / 1024 / 1024}MB",
                    code = "FILE_TOO_LARGE"
                )
            }

            // Step 2: Downloadsフォルダに一時保存
            val downloadsDir = File("/host/Downloads")
            if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "Downloadsフォルダにアクセスできません: /host/Downloads",
                    code = "DOWNLOADS_NOT_ACCESSIBLE"
                )
            }

            tempFile = File(downloadsDir, fileName)

            // 同名ファイルが既に存在する場合は上書き警告
            val fileExisted = tempFile.exists()

            tempFile.writeBytes(fileBytes)

            // Step 3: MIMEタイプの自動検出
            val detectedMimeType = mimeType ?: detectMimeTypeFromExtension(fileName)

            // Step 4: Doorayドライブにアップロード
            val uploadRequest = Base64UploadRequest(
                fileName = fileName,
                base64Content = base64Content,
                parentId = parentId,
                mimeType = detectedMimeType
            )

            val response = doorayClient.uploadFileFromBase64(driveId, uploadRequest)

            if (response.header.isSuccessful && response.result != null) {
                // Step 5: 成功後の処理
                if (!keepLocal) {
                    // 一時ファイルを削除
                    tempFile.delete()
                }

                val successMessage = buildString {
                    append("✅ ファイル '$fileName' (${fileBytes.size / 1024}KB) をDoorayドライブに正常にアップロードしました。")
                    if (fileExisted) {
                        append("\n⚠️ 既存の同名ファイルを上書きしました。")
                    }
                    if (keepLocal) {
                        append("\n💾 ファイルはDownloadsフォルダに保存されています: /Users/jp17463/Downloads/$fileName")
                    } else {
                        append("\n🗑️ 一時ファイルを削除しました。")
                    }
                }

                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = successMessage
                )

                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                // アップロード失敗時は一時ファイルを削除
                if (tempFile?.exists() == true && !keepLocal) {
                    tempFile.delete()
                }

                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = response.header.resultMessage,
                    code = "DOORAY_API_${response.header.resultCode}"
                ).toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            }
        } catch (e: ToolException) {
            // エラー時は一時ファイルを削除
            tempFile?.delete()
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            // エラー時は一時ファイルを削除
            tempFile?.delete()

            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "ファイルアップロード中にエラーが発生しました: ${e.message}",
                code = "UPLOAD_FILE_WITH_SAVE_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}

/**
 * ファイル拡張子からMIMEタイプを推測します
 */
private fun detectMimeTypeFromExtension(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "txt" -> "text/plain"
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "zip" -> "application/zip"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
    }
}
