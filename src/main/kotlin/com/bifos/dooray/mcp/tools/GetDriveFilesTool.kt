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

fun getDriveFilesTool(): Tool {
    return Tool(
        name = "dooray_drive_list_files",
        description = "특정 드라이브의 파일 목록을 조회합니다. 폴더 구조를 탐색할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("parent_id") {
                    put("type", "string")
                    put("description", "상위 폴더 ID (선택사항, 루트는 null)")
                }
                putJsonObject("page") {
                    put("type", "integer")
                    put("description", "페이지 번호 (기본값: 0)")
                    put("default", 0)
                }
                putJsonObject("size") {
                    put("type", "integer")
                    put("description", "페이지당 항목 수 (기본값: 50)")
                    put("default", 50)
                }
            }),
        outputSchema = null,
        annotations = null
    )
}

fun getDriveFilesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )
            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50
            
            val response = doorayClient.getDriveFiles(driveId, parentId, page, size)
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 드라이브 파일 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
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
                message = "드라이브 파일 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_DRIVE_FILES_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}