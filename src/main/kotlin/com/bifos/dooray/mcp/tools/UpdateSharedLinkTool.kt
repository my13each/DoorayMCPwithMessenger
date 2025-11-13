package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.types.UpdateSharedLinkRequest
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

fun updateSharedLinkTool(): Tool {
    return Tool(
        name = "dooray_drive_update_shared_link",
        description = """
            특정 공유 링크를 수정합니다.

            📌 수정 가능 항목:
            - expiredAt: 만료 날짜 (ISO 8601 형식)
            - scope: 공유 범위 (member | memberAndGuest | memberAndGuestAndExternal)
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
put("drive_id", buildJsonObject {
                    put("type", "string")
                    put("description", "드라이브 ID")
                })
                put("file_id", buildJsonObject {
                    put("type", "string")
                    put("description", "파일 ID")
                })
                put("link_id", buildJsonObject {
                    put("type", "string")
                    put("description", "공유 링크 ID")
                })
                put("expired_at", buildJsonObject {
                    put("type", "string")
                    put("description", "만료 날짜 (ISO 8601 형식, 예: 2025-12-31T23:59:59+09:00)")
                })
                put("scope", buildJsonObject {
                    put("type", "string")
                    put("description", "공유 범위: member | memberAndGuest | memberAndGuestAndExternal")
                    put("enum", buildJsonObject {
                        put("member", "손님 제외 조직 내 사용자")
                        put("memberAndGuest", "조직 내 모든 사용자")
                        put("memberAndGuestAndExternal", "내외부 상관없이")
                    })
                })
                }
                putJsonArray("required") {
                    add(JsonPrimitive("drive_id"))
                    add(JsonPrimitive("file_id"))
                    add(JsonPrimitive("link_id"))
                    add(JsonPrimitive("expired_at"))
                    add(JsonPrimitive("scope"))
                }
            }),
        outputSchema = null,
        annotations = null
    )
}

fun updateSharedLinkHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "drive_id는 필수입니다", "MISSING_DRIVE_ID")

            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "file_id는 필수입니다", "MISSING_FILE_ID")

            val linkId = request.arguments["link_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "link_id는 필수입니다", "MISSING_LINK_ID")

            val expiredAt = request.arguments["expired_at"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "expired_at는 필수입니다", "MISSING_EXPIRED_AT")

            val scope = request.arguments["scope"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "scope는 필수입니다", "MISSING_SCOPE")

            // scope 유효성 검사
            if (scope !in listOf("member", "memberAndGuest", "memberAndGuestAndExternal")) {
                throw ToolException(
                    ToolException.VALIDATION_ERROR,
                    "scope는 member, memberAndGuest, memberAndGuestAndExternal 중 하나여야 합니다",
                    "INVALID_SCOPE"
                )
            }

            val updateRequest = UpdateSharedLinkRequest(
                expiredAt = expiredAt,
                scope = scope
            )

            val response = doorayClient.updateSharedLink(driveId, fileId, linkId, updateRequest)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = null,
                    message = "✅ 공유 링크를 성공적으로 수정했습니다"
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
                message = "공유 링크 수정 중 오류가 발생했습니다: ${e.message}",
                code = "UPDATE_SHARED_LINK_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
