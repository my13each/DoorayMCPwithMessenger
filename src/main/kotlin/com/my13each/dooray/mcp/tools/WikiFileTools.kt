package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import java.io.File

/** 위키 파일 업로드 최대 크기 */
private const val WIKI_UPLOAD_MAX_BYTES = 100L * 1024 * 1024

// ============ 파일 다운로드 ============

fun downloadWikiFileTool(): Tool = Tool(
    name = "dooray_wiki_download_file",
    description = """
        두레이 위키 페이지의 첨부 파일 또는 인라인 이미지를 다운로드해 로컬에 저장합니다.
        - 페이지 상세(dooray_wiki_get_page)의 files[] / images[] 항목을 사용합니다.
        - page_id + file_id(files[].id)로 받거나, attach_file_id(files[].attachFileId)만으로 받을 수 있습니다.
        - 저장 위치를 생략하면 Downloads 폴더에 저장됩니다 (Docker 실행 시 /host/Downloads = 호스트의 ~/Downloads).
        - 텍스트 파일(50KB 이하)은 내용도 응답에 포함됩니다.
    """.trimIndent(),
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", "파일이 첨부된 $PAGE_ID_DESC (file_id 사용 시 필수)")
            stringProp("file_id", "페이지 파일 ID (files[].id 또는 images[].id)")
            stringProp("attach_file_id", "첨부 파일 ID (files[].attachFileId 또는 images[].attachFileId). file_id 대신 사용 가능")
            stringProp("save_dir", "저장할 디렉터리 (선택, 기본값: Downloads)")
        },
        required = listOf("wiki_id")
    ),
    outputSchema = null,
    annotations = null
)

fun downloadWikiFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 파일 다운로드", "DOWNLOAD_WIKI_FILE", listOf("wiki_id")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")
        val fileId = request.stringArg("file_id")
        val attachFileId = request.stringArg("attach_file_id")

        val download = when {
            fileId != null && pageId != null -> doorayClient.downloadWikiPageFile(wikiId, pageId, fileId)
            attachFileId != null -> doorayClient.downloadWikiAttachFile(wikiId, attachFileId)
            else -> return@wikiToolHandler toolError(
                ToolException.PARAMETER_MISSING,
                "page_id + file_id 또는 attach_file_id 중 하나가 필요합니다.",
                "MISSING_FILE_ID"
            )
        }
        val saved = saveDownloadedFile(download, request.stringArg("save_dir"), "dooray_wiki_file_${fileId ?: attachFileId}")

        toolSuccess(buildJsonObject {
            put("wiki_id", wikiId)
            put("file_name", saved.fileName)
            put("saved_path", saved.savedPath)
            put("host_path_hint", saved.hostPathHint)
            put("size", saved.size)
            put("content_type", download.contentType)
            put("text_content", saved.textContent)
        }, "📥 파일을 저장했습니다: ${saved.savedPath} (${saved.size} bytes)")
    }

// ============ 파일 업로드 ============

fun uploadWikiFileTool(): Tool = Tool(
    name = "dooray_wiki_upload_file",
    description = """
        로컬 파일 1개를 기존 두레이 위키 페이지에 업로드합니다 (최대 100MB).
        - type=general: 일반 첨부 파일 (기본값)
        - type=inline_image: 본문에 넣을 이미지. 응답의 markdown 값을 dooray_wiki_update_page_content로 본문에 넣으면 이미지가 표시됩니다.
        - Docker 실행 시 /Users/{user}/Downloads, /Users/{user}/Desktop 경로는 자동 변환됩니다.
    """.trimIndent(),
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", "파일을 첨부할 $PAGE_ID_DESC")
            stringProp("file_path", "업로드할 로컬 파일의 절대 경로")
            putJsonObject("type") {
                put("type", "string")
                put("description", "general(일반 첨부) 또는 inline_image(본문 이미지). 기본값: general")
                putJsonArray("enum") { add("general"); add("inline_image") }
            }
        },
        required = listOf("wiki_id", "page_id", "file_path")
    ),
    outputSchema = null,
    annotations = null
)

fun uploadWikiFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 파일 업로드", "UPLOAD_WIKI_FILE", listOf("wiki_id", "page_id", "file_path")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val type = request.stringArg("type") ?: "general"
        if (type != "general" && type != "inline_image") {
            return@wikiToolHandler toolError(ToolException.VALIDATION_ERROR, "type은 general 또는 inline_image만 가능합니다: $type", "INVALID_TYPE")
        }

        val file = File(convertHostPathToContainerPath(request.stringArg("file_path")!!))
        when {
            !file.isFile -> return@wikiToolHandler toolError(ToolException.VALIDATION_ERROR, "파일을 찾을 수 없습니다: ${file.path}", "FILE_NOT_FOUND")
            file.length() > WIKI_UPLOAD_MAX_BYTES -> return@wikiToolHandler toolError(ToolException.VALIDATION_ERROR, "파일이 100MB를 넘습니다: ${file.length()} bytes", "FILE_TOO_LARGE")
        }

        val response = doorayClient.uploadWikiPageFile(
            wikiId = wikiId,
            pageId = pageId,
            type = type,
            fileName = file.name,
            fileContent = file.readBytes(),
            mimeType = detectMimeType(file.name)
        )
        val uploaded = response.result
        if (response.header.isSuccessful && uploaded != null) {
            val attachFileId = uploaded.attachFileId ?: uploaded.id
            toolSuccess(buildJsonObject {
                put("wiki_id", wikiId)
                put("page_id", pageId)
                put("file_id", uploaded.id)
                put("attach_file_id", attachFileId)
                put("name", uploaded.name ?: file.name)
                put("type", uploaded.type ?: type)
                put("size", uploaded.size ?: file.length())
                if (type == "inline_image") {
                    put("markdown", "![${uploaded.name ?: file.name}](/wikis/$wikiId/files/$attachFileId)")
                }
            }, "📤 위키 페이지에 파일을 업로드했습니다: ${uploaded.name ?: file.name}")
        } else apiFailure("위키 파일 업로드", response.header, "UPLOAD_WIKI_FILE_FAILED")
    }

// ============ 파일 삭제 ============

fun deleteWikiFileTool(): Tool = Tool(
    name = "dooray_wiki_delete_file",
    description = "두레이 위키 페이지에 첨부된 파일(또는 인라인 이미지)을 삭제합니다. file_id는 페이지 상세의 files[].id / images[].id입니다. ⚠️ 되돌릴 수 없습니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("file_id", "삭제할 페이지 파일 ID (files[].id 또는 images[].id)")
        },
        required = listOf("wiki_id", "page_id", "file_id")
    ),
    outputSchema = null,
    annotations = null
)

fun deleteWikiFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 파일 삭제", "DELETE_WIKI_FILE", listOf("wiki_id", "page_id", "file_id")) { request ->
        val fileId = request.stringArg("file_id")!!
        val response = doorayClient.deleteWikiPageFile(request.stringArg("wiki_id")!!, request.stringArg("page_id")!!, fileId)
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("file_id", fileId) }, "🗑️ 위키 페이지 파일을 삭제했습니다.")
        } else apiFailure("위키 파일 삭제", response.header, "DELETE_WIKI_FILE_FAILED")
    }
