package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonPrimitive

fun getSharedLinkDetailTool(): Tool {
    return Tool(
        name = "dooray_drive_get_shared_link_detail",
        description = """
            특정 공유 링크의 상세 정보를 조회합니다.

            📌 조회 정보:
            - 링크 ID, 생성일시, 만료일시
            - 생성자 정보
            - 실제 공유 링크 URL
            - 공유 범위 (scope)
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("drive_id") {
                        put("type", "string")
                        put("description", "드라이브 ID")
                    }
                    putJsonObject("file_id") {
                        put("type", "string")
                        put("description", "파일 ID")
                    }
                    putJsonObject("link_id") {
                        put("type", "string")
                        put("description", "공유 링크 ID")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("drive_id"))
                    add(JsonPrimitive("file_id"))
                    add(JsonPrimitive("link_id"))
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getSharedLinkDetailHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "drive_id는 필수입니다", "MISSING_DRIVE_ID")

            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "file_id는 필수입니다", "MISSING_FILE_ID")

            val linkId = request.arguments["link_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "link_id는 필수입니다", "MISSING_LINK_ID")

            val response = doorayClient.getSharedLinkDetail(driveId, fileId, linkId)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 공유 링크 상세 정보를 성공적으로 조회했습니다"
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
                message = "공유 링크 상세 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_SHARED_LINK_DETAIL_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
