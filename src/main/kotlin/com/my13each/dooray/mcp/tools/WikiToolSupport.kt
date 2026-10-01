package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.LenientApiHeader
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// 위키 페이지 관리 · 댓글 · 파일 도구가 공유하는 헬퍼

internal const val WIKI_ID_DESC = "위키 ID (dooray_wiki_list_projects로 조회 가능)"
internal const val PAGE_ID_DESC = "위키 페이지 ID (dooray_wiki_list_pages로 조회 가능)"

internal fun JsonObjectBuilder.stringProp(name: String, description: String) =
    putJsonObject(name) {
        put("type", "string")
        put("description", description)
    }

internal fun CallToolRequest.stringArg(name: String): String? =
    arguments[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun CallToolRequest.intArg(name: String): Int? =
    arguments[name]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

internal fun CallToolRequest.boolArg(name: String): Boolean? =
    arguments[name]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

internal fun toolSuccess(data: JsonObject, message: String): CallToolResult =
    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(ToolSuccessResponse(data = data, message = message)))))

internal fun toolError(type: String, message: String, code: String): CallToolResult =
    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(ToolException(type = type, message = message, code = code).toErrorResponse()))))

internal fun apiFailure(action: String, header: LenientApiHeader, code: String): CallToolResult =
    toolError(ToolException.API_ERROR, "$action 실패: ${header.resultMessage ?: "resultCode=${header.resultCode}"}", code)

/**
 * 필수 파라미터 검증과 예외 처리를 공통으로 감싼 핸들러.
 * [required]가 하나라도 비어 있으면 PARAMETER_MISSING을 반환합니다.
 */
internal fun wikiToolHandler(
    action: String,
    errorCode: String,
    required: List<String>,
    block: suspend (CallToolRequest) -> CallToolResult
): suspend (CallToolRequest) -> CallToolResult = { request ->
    val missing = required.firstOrNull { request.stringArg(it) == null }
    if (missing != null) {
        toolError(ToolException.PARAMETER_MISSING, "$missing 파라미터가 필요합니다.", "MISSING_${missing.uppercase()}")
    } else {
        try {
            block(request)
        } catch (e: ToolException) {
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            toolError(ToolException.INTERNAL_ERROR, "$action 중 오류가 발생했습니다: ${e.message}", "${errorCode}_ERROR")
        }
    }
}
