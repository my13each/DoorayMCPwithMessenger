package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.types.WikiCommentBody
import com.my13each.dooray.mcp.types.WikiCommentRequest
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val COMMENT_ID_DESC = "위키 댓글 ID (dooray_wiki_list_comments로 조회 가능)"

// ============ 댓글 작성 ============

fun createWikiCommentTool(): Tool = Tool(
    name = "dooray_wiki_create_comment",
    description = "두레이 위키 페이지에 댓글을 작성합니다. 본문은 Markdown 형식입니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("content", "댓글 내용 (Markdown)")
        },
        required = listOf("wiki_id", "page_id", "content")
    ),
    outputSchema = null,
    annotations = null
)

fun createWikiCommentHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 댓글 작성", "CREATE_WIKI_COMMENT", listOf("wiki_id", "page_id", "content")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val content = request.stringArg("content")!!
        val response = doorayClient.createWikiComment(wikiId, pageId, WikiCommentRequest(WikiCommentBody(content = content)))
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject {
                put("wiki_id", wikiId)
                put("page_id", pageId)
                put("comment_id", response.result?.id)
            }, "💬 위키 댓글을 작성했습니다.")
        } else apiFailure("위키 댓글 작성", response.header, "CREATE_WIKI_COMMENT_FAILED")
    }

// ============ 댓글 목록 ============

fun getWikiCommentsTool(): Tool = Tool(
    name = "dooray_wiki_list_comments",
    description = "두레이 위키 페이지의 댓글 목록을 조회합니다. 최신순이며 page=0의 첫 항목이 가장 최근 댓글입니다. 작성자 이름(creator.member.name)이 포함됩니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            putJsonObject("page") {
                put("type", "integer")
                put("description", "페이지 번호 (기본값: 0)")
            }
            putJsonObject("size") {
                put("type", "integer")
                put("description", "페이지 크기 (기본값: 20, 최대: 100)")
            }
        },
        required = listOf("wiki_id", "page_id")
    ),
    outputSchema = null,
    annotations = null
)

fun getWikiCommentsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 댓글 목록 조회", "GET_WIKI_COMMENTS", listOf("wiki_id", "page_id")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val page = request.intArg("page")
        val size = request.intArg("size")?.coerceIn(1, 100)
        val response = doorayClient.getWikiComments(wikiId, pageId, page, size)
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject {
                put("wiki_id", wikiId)
                put("page_id", pageId)
                put("total_count", response.totalCount ?: response.result.size)
                put("comments", JsonUtils.toJsonElement(response.result))
            }, "💬 위키 댓글 ${response.result.size}개를 조회했습니다 (전체 ${response.totalCount ?: response.result.size}개).")
        } else apiFailure("위키 댓글 목록 조회", response.header, "GET_WIKI_COMMENTS_FAILED")
    }

// ============ 댓글 1건 조회 ============

fun getWikiCommentTool(): Tool = Tool(
    name = "dooray_wiki_get_comment",
    description = "두레이 위키 댓글 1건을 조회합니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("comment_id", COMMENT_ID_DESC)
        },
        required = listOf("wiki_id", "page_id", "comment_id")
    ),
    outputSchema = null,
    annotations = null
)

fun getWikiCommentHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 댓글 조회", "GET_WIKI_COMMENT", listOf("wiki_id", "page_id", "comment_id")) { request ->
        val response = doorayClient.getWikiComment(
            request.stringArg("wiki_id")!!, request.stringArg("page_id")!!, request.stringArg("comment_id")!!
        )
        val comment = response.result
        if (response.header.isSuccessful && comment != null) {
            toolSuccess(buildJsonObject { put("comment", JsonUtils.toJsonElement(comment)) }, "💬 위키 댓글을 조회했습니다.")
        } else apiFailure("위키 댓글 조회", response.header, "GET_WIKI_COMMENT_FAILED")
    }

// ============ 댓글 수정 ============

fun updateWikiCommentTool(): Tool = Tool(
    name = "dooray_wiki_update_comment",
    description = "두레이 위키 댓글 내용을 수정합니다 (Markdown).",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("comment_id", COMMENT_ID_DESC)
            stringProp("content", "새 댓글 내용 (Markdown)")
        },
        required = listOf("wiki_id", "page_id", "comment_id", "content")
    ),
    outputSchema = null,
    annotations = null
)

fun updateWikiCommentHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 댓글 수정", "UPDATE_WIKI_COMMENT", listOf("wiki_id", "page_id", "comment_id", "content")) { request ->
        val commentId = request.stringArg("comment_id")!!
        val response = doorayClient.updateWikiComment(
            request.stringArg("wiki_id")!!, request.stringArg("page_id")!!, commentId,
            WikiCommentRequest(WikiCommentBody(content = request.stringArg("content")!!))
        )
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("comment_id", commentId) }, "✏️ 위키 댓글을 수정했습니다.")
        } else apiFailure("위키 댓글 수정", response.header, "UPDATE_WIKI_COMMENT_FAILED")
    }

// ============ 댓글 삭제 ============

fun deleteWikiCommentTool(): Tool = Tool(
    name = "dooray_wiki_delete_comment",
    description = "두레이 위키 댓글을 삭제합니다. ⚠️ 되돌릴 수 없습니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("comment_id", COMMENT_ID_DESC)
        },
        required = listOf("wiki_id", "page_id", "comment_id")
    ),
    outputSchema = null,
    annotations = null
)

fun deleteWikiCommentHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 댓글 삭제", "DELETE_WIKI_COMMENT", listOf("wiki_id", "page_id", "comment_id")) { request ->
        val commentId = request.stringArg("comment_id")!!
        val response = doorayClient.deleteWikiComment(request.stringArg("wiki_id")!!, request.stringArg("page_id")!!, commentId)
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("comment_id", commentId) }, "🗑️ 위키 댓글을 삭제했습니다.")
        } else apiFailure("위키 댓글 삭제", response.header, "DELETE_WIKI_COMMENT_FAILED")
    }
