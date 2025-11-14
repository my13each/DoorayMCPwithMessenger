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

fun getSharedLinksTool(): Tool {
    return Tool(
        name = "dooray_drive_get_shared_links",
        description = """
            파일에 생성된 모든 공유 링크를 조회합니다.

            📌 참고:
            - 요청자의 권한이 project admin이면 해당 파일에 생성된 링크 전체를 응답합니다.
            - 그 외의 경우는 자신이 생성한 정보만 응답합니다.
            - valid: true (유효한 링크, 기본값), false (만료된 링크)
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
                    putJsonObject("valid") {
                        put("type", "boolean")
                        put("description", "true: 유효한 링크(기본값), false: 만료된 링크")
                    }
                }

                putJsonArray("required") {
                    add("drive_id")
                    add("file_id")
                }
                put("additionalProperties", false)
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getSharedLinksHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "drive_id는 필수입니다", "MISSING_DRIVE_ID")

            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "file_id는 필수입니다", "MISSING_FILE_ID")

            val valid = request.arguments["valid"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val response = doorayClient.getSharedLinks(driveId, fileId, valid)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 공유 링크 목록을 성공적으로 조회했습니다 (총 ${response.totalCount ?: response.result.size}개)"
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
                message = "공유 링크 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_SHARED_LINKS_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
