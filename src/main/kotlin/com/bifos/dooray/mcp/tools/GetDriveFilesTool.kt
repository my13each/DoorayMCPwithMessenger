package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DriveFileListResponseData
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
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
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDriveFilesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(message = "drive_id는 필수 매개변수입니다.", code = "MISSING_DRIVE_ID")
            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50
            
            val response = doorayClient.getDriveFiles(driveId, parentId, page, size)
            
            if (response.header.isSuccessful) {
                val responseData = DriveFileListResponseData(
                    files = response.result,
                    totalCount = response.totalCount ?: response.result.size,
                    driveId = driveId
                )
                
                CallToolResult(
                    content = listOf(
                        JsonUtils.createJsonContent(responseData)
                    ),
                    isError = false
                )
            } else {
                throw ToolException(
                    message = "드라이브 파일 목록 조회 실패: ${response.header.resultMessage}",
                    code = "GET_DRIVE_FILES_ERROR"
                )
            }
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    JsonUtils.createTextContent("드라이브 파일 목록 조회 중 오류가 발생했습니다: ${e.message}")
                ),
                isError = true
            )
        }
    }
}