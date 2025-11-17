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

fun getDriveDetailTool(): Tool {
    return Tool(
        name = "dooray_drive_get_detail",
        description = """
            특정 드라이브의 상세 정보를 조회합니다.
            드라이브 타입(개인/프로젝트), 멤버 목록, 역할 등을 확인할 수 있습니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("drive_id", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("조회할 드라이브 ID"))
                })
            },
            required = listOf("drive_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDriveDetailHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )

            val response = doorayClient.getDriveDetail(driveId)

            if (response.header.isSuccessful && response.result != null) {
                val driveInfo = response.result
                val driveType = driveInfo.type ?: "unknown"
                val driveName = driveInfo.name ?: "(개인 드라이브)"
                val memberCount = driveInfo.members?.size ?: 0

                val successResponse = ToolSuccessResponse(
                    data = driveInfo,
                    message = "✅ 드라이브 상세 정보를 성공적으로 조회했습니다. " +
                            "타입: $driveType, 이름: $driveName, 멤버: ${memberCount}명"
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
                message = "드라이브 상세 정보 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_DRIVE_DETAIL_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
