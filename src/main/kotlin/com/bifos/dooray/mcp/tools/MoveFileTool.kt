package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.MoveFileRequest
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

fun moveFileTool(): Tool {
    return Tool(
        name = "dooray_drive_move_file",
        description = "드라이브 파일을 다른 폴더로 이동합니다. 원본 파일은 새 위치로 이동되며 기존 위치에서는 사라집니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "파일이 있는 드라이브 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "이동할 파일 ID")
                }
                putJsonObject("destination_folder_id") {
                    put("type", "string")
                    put("description", "이동될 대상 폴더 ID")
                }
                }

                putJsonArray("required") {
                    add("drive_id")
                    add("file_id")
                    add("destination_folder_id")
                }
                put("additionalProperties", false)
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun moveFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )
            val fileId = request.arguments["file_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "file_id는 필수 매개변수입니다.",
                    code = "MISSING_FILE_ID"
                )
            val destinationFolderId = request.arguments["destination_folder_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "destination_folder_id는 필수 매개변수입니다.",
                    code = "MISSING_DESTINATION_FOLDER_ID"
                )
            
            val moveRequest = MoveFileRequest(destinationFileId = destinationFolderId)
            val response = doorayClient.moveFile(driveId, fileId, moveRequest)
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = mapOf(
                        "driveId" to driveId,
                        "fileId" to fileId,
                        "destinationFolderId" to destinationFolderId,
                        "moved" to true
                    ),
                    message = "✅ 파일을 성공적으로 이동했습니다."
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
                message = "파일 이동 중 오류가 발생했습니다: ${e.message}",
                code = "MOVE_FILE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}