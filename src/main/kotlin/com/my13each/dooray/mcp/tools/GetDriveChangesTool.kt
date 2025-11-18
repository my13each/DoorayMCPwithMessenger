package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getDriveChangesTool(): Tool {
    return Tool(
        name = "dooray_drive_get_changes",
        description = """
            드라이브 내에 발생한 변경사항을 조회합니다.
            파일/폴더의 생성, 수정, 삭제 이력을 추적할 수 있습니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("드라이브 ID"))
                }
                putJsonObject("latest_revision") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("조회할 변경사항 기준 점 (기본값: 0)"))
                }
                putJsonObject("file_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("latestRevision과 함께 사용하여 특정 파일 이후의 변경사항 조회"))
                }
                putJsonObject("size") {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("조회할 개수 (기본값: 20, 최대: 200)"))
                    put("default", JsonPrimitive(20))
                }
            },
            required = listOf("drive_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDriveChangesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )
            val latestRevision = request.arguments["latest_revision"]?.jsonPrimitive?.content
            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull()

            val response = doorayClient.getDriveChanges(driveId, latestRevision, fileId, size)

            if (response.header.isSuccessful) {
                val changes = response.result
                val updatedCount = changes.count { it.changeType == "updated" }
                val deletedCount = changes.count { it.changeType == "deleted" }

                val message = "✅ 드라이브 변경사항을 성공적으로 조회했습니다. " +
                        "(총 ${changes.size}개: 수정 ${updatedCount}개, 삭제 ${deletedCount}개)"

                val successResponse = ToolSuccessResponse(
                    data = changes,
                    message = message
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
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "드라이브 변경사항 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_DRIVE_CHANGES_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
