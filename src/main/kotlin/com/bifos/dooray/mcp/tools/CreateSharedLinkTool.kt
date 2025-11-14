package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CreateSharedLinkRequest
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

fun createSharedLinkTool(): Tool {
    return Tool(
        name = "dooray_drive_create_shared_link",
        description = """
            드라이브 파일의 공유 링크를 생성합니다.

            📌 참고:
            - 파일 공유 링크는 프로젝트 관리자와 생성자만 생성 가능합니다.
            - scope: "member" (손님 제외 조직 내 사용자), "memberAndGuest" (조직 내 모든 사용자), "memberAndGuestAndExternal" (내외부 상관없이)
            - expiredAt: 만료 날짜 (ISO 8601 형식, 필수)
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
                putJsonObject("scope") {
                    put("type", "string")
                    put("description", "공유 범위 (기본값: memberAndGuest): member | memberAndGuest | memberAndGuestAndExternal")
                    put("default", "memberAndGuest")
                    putJsonObject("enum") {
                        put("member", "손님 제외 조직 내 사용자")
                        put("memberAndGuest", "조직 내 모든 사용자 (기본값)")
                        put("memberAndGuestAndExternal", "내외부 상관없이 (조직 정책으로 차단될 수 있음)")
                    }
                }
                putJsonObject("expired_at") {
                    put("type", "string")
                    put("description", "만료 날짜 (ISO 8601 형식, 예: 2025-12-31T23:59:59+09:00)")
                }
            },
            required = listOf("drive_id", "file_id", "expired_at")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun createSharedLinkHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "drive_id는 필수입니다", "MISSING_DRIVE_ID")

            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "file_id는 필수입니다", "MISSING_FILE_ID")

            val scope = request.arguments["scope"]?.jsonPrimitive?.content
                ?: "memberAndGuest"  // 기본값: 조직 내 모든 사용자 (외부 공유는 조직 정책으로 차단될 수 있음)

            val expiredAt = request.arguments["expired_at"]?.jsonPrimitive?.content
                ?: throw ToolException(ToolException.VALIDATION_ERROR, "expired_at는 필수입니다", "MISSING_EXPIRED_AT")

            // scope 유효성 검사
            if (scope !in listOf("member", "memberAndGuest", "memberAndGuestAndExternal")) {
                throw ToolException(
                    ToolException.VALIDATION_ERROR,
                    "scope는 member, memberAndGuest, memberAndGuestAndExternal 중 하나여야 합니다",
                    "INVALID_SCOPE"
                )
            }

            val createRequest = CreateSharedLinkRequest(
                scope = scope,
                expiredAt = expiredAt
            )

            val response = doorayClient.createSharedLink(driveId, fileId, createRequest)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 공유 링크를 성공적으로 생성했습니다 (링크 ID: ${response.result?.id})"
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
                message = "공유 링크 생성 중 오류가 발생했습니다: ${e.message}",
                code = "CREATE_SHARED_LINK_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
