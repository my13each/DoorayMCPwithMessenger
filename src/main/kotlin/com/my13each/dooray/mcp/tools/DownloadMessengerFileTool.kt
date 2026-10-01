package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.MessengerFileDownloadResponseData
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

/** 텍스트 파일 내용을 응답에 포함할 최대 크기 */
private const val INLINE_TEXT_MAX_BYTES = 50 * 1024

fun downloadMessengerFileTool(): Tool {
    return Tool(
        name = "dooray_messenger_download_file",
        description = """
            두레이 메신저 메시지에 첨부된 파일을 다운로드해 로컬에 저장합니다.

            - file_id는 dooray_messenger_get_channel_logs 결과 메시지의 file.id입니다.
            - 저장 위치를 생략하면 Downloads 폴더에 저장됩니다 (Docker 실행 시 /host/Downloads = 호스트의 ~/Downloads).
            - 텍스트 파일(50KB 이하)은 내용도 응답에 포함됩니다.
            - ⚠️ 메신저 첨부 파일은 업로드 후 약 2주가 지나면 만료되어 받을 수 없습니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "파일이 첨부된 메시지가 있는 채널 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "첨부 파일 ID (메시지의 file.id)")
                }
                putJsonObject("save_dir") {
                    put("type", "string")
                    put("description", "저장할 디렉터리 (선택, 기본값: Downloads). 예: /Users/{user}/Downloads/dooray")
                }
            },
            required = listOf("channel_id", "file_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun downloadMessengerFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
            val saveDirArg = request.arguments["save_dir"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

            val missing = when {
                channelId.isNullOrBlank() -> "channel_id"
                fileId.isNullOrBlank() -> "file_id"
                else -> null
            }

            if (missing != null) {
                val errorResponse = ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "$missing 파라미터가 필요합니다.",
                    code = "MISSING_${missing.uppercase()}"
                ).toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            } else {
                val download = doorayClient.downloadMessengerFile(channelId!!, fileId!!)

                val saveDir = File(saveDirArg?.let { convertHostPathToContainerPath(it) } ?: defaultDownloadDir())
                saveDir.mkdirs()
                val target = uniqueFile(saveDir, sanitizeFileName(download.fileName ?: "dooray_file_$fileId"))
                target.writeBytes(download.bytes)

                val isText = download.contentType?.let {
                    it.startsWith("text/") || it.contains("json") || it.contains("xml")
                } ?: false
                val inlineText = if (isText && download.bytes.size <= INLINE_TEXT_MAX_BYTES) {
                    download.bytes.toString(Charsets.UTF_8)
                } else null

                val successResponse = ToolSuccessResponse(
                    data = MessengerFileDownloadResponseData(
                        channelId = channelId,
                        fileId = fileId,
                        fileName = target.name,
                        savedPath = target.absolutePath,
                        hostPathHint = containerPathToHostHint(target.absolutePath),
                        size = download.bytes.size.toLong(),
                        contentType = download.contentType,
                        textContent = inlineText
                    ),
                    message = "📥 파일을 저장했습니다: ${target.absolutePath} (${download.bytes.size} bytes)"
                )
                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
            }
        } catch (e: ToolException) {
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "메신저 파일 다운로드 중 오류가 발생했습니다: ${e.message}",
                code = "DOWNLOAD_MESSENGER_FILE_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}

/** Docker 실행 시 /host/Downloads, 로컬 실행 시 ~/Downloads */
private fun defaultDownloadDir(): String {
    val dockerDownloads = File("/host/Downloads")
    return if (dockerDownloads.isDirectory) dockerDownloads.path
    else File(System.getProperty("user.home"), "Downloads").path
}

/** 컨테이너 경로를 호스트 경로 힌트로 변환 (/host/Downloads/a.txt → ~/Downloads/a.txt) */
private fun containerPathToHostHint(path: String): String? = when {
    path.startsWith("/host/Downloads") -> path.replaceFirst("/host/Downloads", "~/Downloads")
    path.startsWith("/host/Desktop") -> path.replaceFirst("/host/Desktop", "~/Desktop")
    path.startsWith("/home/claude") -> path.replaceFirst("/home/claude", "~/Downloads")
    else -> null
}

private fun sanitizeFileName(name: String): String =
    name.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[\\u0000-\\u001f]"), "_").ifBlank { "dooray_file" }

/** 같은 이름이 있으면 "name (1).ext" 형태로 회피 */
private fun uniqueFile(dir: File, name: String): File {
    var candidate = File(dir, name)
    if (!candidate.exists()) return candidate
    val base = name.substringBeforeLast('.', name)
    val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || it == name) "" else ".$it" }
    var i = 1
    while (candidate.exists()) {
        candidate = File(dir, "$base ($i)$ext")
        i++
    }
    return candidate
}
