package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.*
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add

// ============ 페이지 삭제 ============

fun deleteWikiPageTool(): Tool = Tool(
    name = "dooray_wiki_delete_page",
    description = "두레이 위키 페이지 1건을 삭제합니다. ⚠️ 되돌릴 수 없으므로 사용자에게 확인 후 실행하세요.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", "삭제할 $PAGE_ID_DESC")
        },
        required = listOf("wiki_id", "page_id")
    ),
    outputSchema = null,
    annotations = null
)

fun deleteWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 페이지 삭제", "DELETE_WIKI_PAGE", listOf("wiki_id", "page_id")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val response = doorayClient.deleteWikiPage(wikiId, pageId)
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("wiki_id", wikiId); put("page_id", pageId) }, "🗑️ 위키 페이지를 삭제했습니다.")
        } else apiFailure("위키 페이지 삭제", response.header, "DELETE_WIKI_PAGE_FAILED")
    }

// ============ 페이지 이동 ============

fun moveWikiPageTool(): Tool = Tool(
    name = "dooray_wiki_move_page",
    description = """
        두레이 위키 페이지의 순서를 바꾸거나, 다른 부모 페이지 또는 다른 위키 아래로 이동합니다.
        - 같은 부모 안에서 순서만 바꾸려면 target_parent_page_id에 현재 부모 ID를 넣고 before_page_id를 지정하세요.
        - before_page_id: 이 페이지 바로 뒤에 위치시킴. 생략하면 순서 변경 없음, "0"이면 맨 앞으로 이동.
    """.trimIndent(),
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", "이동시킬 페이지가 속한 $WIKI_ID_DESC")
            stringProp("page_id", "이동시킬 $PAGE_ID_DESC")
            stringProp("target_parent_page_id", "이동 대상 부모 페이지 ID (필수)")
            stringProp("target_wiki_id", "다른 위키로 옮길 때 대상 위키 ID (선택)")
            putJsonObject("with_children") {
                put("type", "boolean")
                put("description", "하위 페이지도 함께 이동할지 여부 (기본값: true)")
            }
            stringProp("before_page_id", "이 페이지 바로 뒤에 위치시킴 (선택, \"0\"이면 맨 앞)")
        },
        required = listOf("wiki_id", "page_id", "target_parent_page_id")
    ),
    outputSchema = null,
    annotations = null
)

fun moveWikiPageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 페이지 이동", "MOVE_WIKI_PAGE", listOf("wiki_id", "page_id", "target_parent_page_id")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val moveRequest = MoveWikiPageRequest(
            targetParentPageId = request.stringArg("target_parent_page_id")!!,
            targetWikiId = request.stringArg("target_wiki_id"),
            withChildren = request.boolArg("with_children"),
            beforePageId = request.stringArg("before_page_id")
        )
        val response = doorayClient.moveWikiPage(wikiId, pageId, moveRequest)
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject {
                put("wiki_id", wikiId)
                put("page_id", pageId)
                put("target_wiki_id", moveRequest.targetWikiId ?: wikiId)
                put("target_parent_page_id", moveRequest.targetParentPageId)
            }, "📦 위키 페이지를 이동했습니다.")
        } else apiFailure("위키 페이지 이동", response.header, "MOVE_WIKI_PAGE_FAILED")
    }

// ============ 제목만 수정 ============

fun updateWikiPageTitleTool(): Tool = Tool(
    name = "dooray_wiki_update_page_title",
    description = "두레이 위키 페이지의 제목만 수정합니다. 본문과 참조자는 그대로 유지됩니다. 같은 위치에 같은 제목이 있으면 409 오류가 납니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("subject", "새 제목")
        },
        required = listOf("wiki_id", "page_id", "subject")
    ),
    outputSchema = null,
    annotations = null
)

fun updateWikiPageTitleHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 페이지 제목 수정", "UPDATE_WIKI_PAGE_TITLE", listOf("wiki_id", "page_id", "subject")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val subject = request.stringArg("subject")!!
        val response = doorayClient.updateWikiPageTitle(wikiId, pageId, UpdateWikiPageTitleRequest(subject))
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("wiki_id", wikiId); put("page_id", pageId); put("subject", subject) }, "✏️ 위키 페이지 제목을 수정했습니다.")
        } else apiFailure("위키 페이지 제목 수정", response.header, "UPDATE_WIKI_PAGE_TITLE_FAILED")
    }

// ============ 본문만 수정 ============

fun updateWikiPageContentTool(): Tool = Tool(
    name = "dooray_wiki_update_page_content",
    description = "두레이 위키 페이지의 본문만 수정합니다 (Markdown). 제목과 참조자는 그대로 유지됩니다.",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            stringProp("body", "새 본문 (Markdown)")
        },
        required = listOf("wiki_id", "page_id", "body")
    ),
    outputSchema = null,
    annotations = null
)

fun updateWikiPageContentHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 페이지 본문 수정", "UPDATE_WIKI_PAGE_CONTENT", listOf("wiki_id", "page_id", "body")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val body = request.stringArg("body")!!
        val response = doorayClient.updateWikiPageContent(
            wikiId, pageId, UpdateWikiPageContentRequest(WikiPageBody(mimeType = "text/x-markdown", content = body))
        )
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject { put("wiki_id", wikiId); put("page_id", pageId); put("body_length", body.length) }, "✏️ 위키 페이지 본문을 수정했습니다.")
        } else apiFailure("위키 페이지 본문 수정", response.header, "UPDATE_WIKI_PAGE_CONTENT_FAILED")
    }

// ============ 참조자만 수정 ============

fun updateWikiPageReferrersTool(): Tool = Tool(
    name = "dooray_wiki_update_page_referrers",
    description = "두레이 위키 페이지의 참조자만 수정합니다. ⚠️ 기존 참조자는 모두 지워지고 입력한 목록으로 덮어씁니다 (빈 배열이면 전부 제거).",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            stringProp("wiki_id", WIKI_ID_DESC)
            stringProp("page_id", PAGE_ID_DESC)
            putJsonObject("referrer_member_ids") {
                put("type", "array")
                put("description", "참조자로 설정할 멤버 ID(organizationMemberId) 목록")
                putJsonObject("items") { put("type", "string") }
            }
        },
        required = listOf("wiki_id", "page_id", "referrer_member_ids")
    ),
    outputSchema = null,
    annotations = null
)

fun updateWikiPageReferrersHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult =
    wikiToolHandler("위키 페이지 참조자 수정", "UPDATE_WIKI_PAGE_REFERRERS", listOf("wiki_id", "page_id")) { request ->
        val wikiId = request.stringArg("wiki_id")!!
        val pageId = request.stringArg("page_id")!!
        val memberIds = request.arguments["referrer_member_ids"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: return@wikiToolHandler toolError(ToolException.PARAMETER_MISSING, "referrer_member_ids 파라미터가 필요합니다.", "MISSING_REFERRER_MEMBER_IDS")
        val referrers = memberIds.map { WikiReferrer(type = "member", member = Member(organizationMemberId = it)) }
        val response = doorayClient.updateWikiPageReferrers(wikiId, pageId, UpdateWikiPageReferrersRequest(referrers))
        if (response.header.isSuccessful) {
            toolSuccess(buildJsonObject {
                put("wiki_id", wikiId)
                put("page_id", pageId)
                putJsonArray("referrer_member_ids") { memberIds.forEach { add(it) } }
            }, "👥 위키 페이지 참조자를 ${memberIds.size}명으로 설정했습니다.")
        } else apiFailure("위키 페이지 참조자 수정", response.header, "UPDATE_WIKI_PAGE_REFERRERS_FAILED")
    }
