package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.Base64UploadRequest
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
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

fun uploadFileFromPathTool(): Tool {
    return Tool(
        name = "dooray_drive_upload_file_from_path",
        description = """
            ローカルファイルシステムからファイルを読み取り、Doorayドライブにアップロードします。

            📌 使用方法:
            - file_pathにローカルファイルの絶対パスを指定
            - ファイルは自動的にBase64エンコードされます
            - 大きなファイル（画像、PDF等）のアップロードに適しています
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "ドライブID")
                }
                putJsonObject("file_path") {
                    put("type", "string")
                    put("description", "アップロードするファイルの絶対パス")
                }
                putJsonObject("parent_id") {
                    put("type", "string")
                    put("description", "親フォルダID（必須）")
                }
                putJsonObject("mime_type") {
                    put("type", "string")
                    put("description", "MIMEタイプ（例: text/plain, image/jpeg, image/png, application/pdf）省略可能")
                }
            },
            required = listOf("drive_id", "file_path", "parent_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun uploadFileFromPathHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_idは必須パラメータです。",
                    code = "MISSING_DRIVE_ID"
                )

            val filePath = request.arguments["file_path"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "file_pathは必須パラメータです。",
                    code = "MISSING_FILE_PATH"
                )

            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "parent_idは必須パラメータです。",
                    code = "MISSING_PARENT_ID"
                )

            val mimeType = request.arguments["mime_type"]?.jsonPrimitive?.content

            // Docker環境での経路自動変換（ホストパス → コンテナパス）
            val convertedPath = convertHostPathToContainerPath(filePath)

            // ファイルの存在確認
            val file = File(convertedPath)
            if (!file.exists()) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "ファイルが見つかりません: $filePath",
                    code = "FILE_NOT_FOUND"
                )
            }

            if (!file.isFile) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "指定されたパスはファイルではありません: $filePath",
                    code = "NOT_A_FILE"
                )
            }

            // ファイルサイズチェック（100MB制限）
            val maxSize = 100 * 1024 * 1024 // 100MB
            if (file.length() > maxSize) {
                throw ToolException(
                    type = ToolException.VALIDATION_ERROR,
                    message = "ファイルサイズが大きすぎます（最大100MB）: ${file.length() / 1024 / 1024}MB",
                    code = "FILE_TOO_LARGE"
                )
            }

            // ファイル読み込みとBase64エンコード
            val fileBytes = file.readBytes()
            val base64Content = Base64.getEncoder().encodeToString(fileBytes)

            // ファイル名とMIMEタイプの自動検出
            val fileName = file.name
            val detectedMimeType = mimeType ?: detectMimeType(fileName)

            val uploadRequest = Base64UploadRequest(
                fileName = fileName,
                base64Content = base64Content,
                parentId = parentId,
                mimeType = detectedMimeType
            )

            val response = doorayClient.uploadFileFromBase64(driveId, uploadRequest)

            if (response.header.isSuccessful && response.result != null) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ ファイル '$fileName' (${file.length() / 1024}KB) をドライブに正常にアップロードしました。"
                )

                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = response.header.resultMessage,
                    code = "DOORAY_API_${response.header.resultCode}"
                ).toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            }
        } catch (e: ToolException) {
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "ファイルアップロード中にエラーが発生しました: ${e.message}",
                code = "UPLOAD_FILE_FROM_PATH_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}

/**
 * ホストマシンのパスをDockerコンテナ内のパスに変換します
 *
 * Docker環境では、ホストの /Users/... パスが /host/... または /home/claude にマウントされているため、
 * 自動的に変換を行います。
 *
 * 例:
 * - /Users/jp17463/Desktop/file.txt → /host/Desktop/file.txt
 * - /Users/jp17463/Downloads/image.png → /host/Downloads/image.png
 * - /home/claude/file.xlsx → /home/claude/file.xlsx (変換不要、Claudeが生成したファイル)
 * - /tmp/file.txt → /tmp/file.txt (変換不要、tmpディレクトリ)
 * - /host/Downloads/file.txt → /host/Downloads/file.txt (変換不要)
 */
internal fun convertHostPathToContainerPath(hostPath: String): String {
    // すでにコンテナパス、Claude作業ディレクトリ、または /tmp の場合はそのまま返す
    if (hostPath.startsWith("/host/") ||
        hostPath.startsWith("/home/claude") ||
        hostPath.startsWith("/tmp/")) {
        return hostPath
    }

    // macOS/Linux の /Users/{username}/Desktop, /Users/{username}/Downloads を変換
    val desktopPattern = Regex("^(/Users/[^/]+/Desktop)(/.*)?$")
    val downloadsPattern = Regex("^(/Users/[^/]+/Downloads)(/.*)?$")

    // ローカル実行（Docker外）では /host マウントが存在しないため変換しない
    return when {
        desktopPattern.matches(hostPath) && !java.io.File("/host/Desktop").isDirectory -> hostPath
        downloadsPattern.matches(hostPath) && !java.io.File("/host/Downloads").isDirectory -> hostPath
        desktopPattern.matches(hostPath) -> {
            hostPath.replaceFirst(Regex("^/Users/[^/]+/Desktop"), "/host/Desktop")
        }
        downloadsPattern.matches(hostPath) -> {
            hostPath.replaceFirst(Regex("^/Users/[^/]+/Downloads"), "/host/Downloads")
        }
        else -> {
            // 変換できない場合は元のパスをそのまま返す（ローカル実行時、/tmp、/home/claude など）
            hostPath
        }
    }
}

/**
 * ファイル拡張子からMIMEタイプを推測します
 */
internal fun detectMimeType(fileName: String): String {
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
