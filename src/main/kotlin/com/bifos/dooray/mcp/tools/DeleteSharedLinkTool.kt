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
import kotlinx.serialization.json.JsonPrimitive

fun deleteSharedLinkTool(): Tool {
    return Tool(
        name = "dooray_drive_delete_shared_link",
        description = """
            특정 공유 링크를 삭제합니다.

            📌 참고:
            - 공유 링크를 삭제하면 해당 링크로는 더 이상 파일에 접근할 수 없습니다.
            - 삭제 작업은 되돌릴 수 없으니 주의해주세요.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
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
                    put("description", "삭제할 공유 링크 ID")
                }
            },
            required = listOf("drive_id", "file_id", "link_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun deleteSharedLinkHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "drive_id는 필수입니다", "MISSING_DRIVE_ID")

            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "file_id는 필수입니다", "MISSING_FILE_ID")

            val linkId = request.arguments["link_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "link_id는 필수입니다", "MISSING_LINK_ID")

            val response = doorayClient.deleteSharedLink(driveId, fileId, linkId)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = null,
                    message = "✅ 공유 링크를 성공적으로 삭제했습니다 (링크 ID: $linkId)"
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
                message = "공유 링크 삭제 중 오류가 발생했습니다: ${e.message}",
                code = "DELETE_SHARED_LINK_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
