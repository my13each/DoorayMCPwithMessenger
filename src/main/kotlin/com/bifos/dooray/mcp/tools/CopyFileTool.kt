package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CopyFileRequest
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

fun copyFileTool(): Tool {
    return Tool(
        name = "dooray_drive_copy_file",
        description = "드라이브 파일을 다른 폴더나 드라이브로 복사합니다. 원본 파일은 그대로 유지되고 복사본이 생성됩니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "원본 파일이 있는 드라이브 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "복사할 파일 ID")
                }
                putJsonObject("destination_drive_id") {
                    put("type", "string")
                    put("description", "복사될 대상 드라이브 ID")
                }
                putJsonObject("destination_folder_id") {
                    put("type", "string")
                    put("description", "복사될 대상 폴더 ID")
                }
            },
            required = listOf("drive_id", "file_id", "destination_drive_id", "destination_folder_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun copyFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
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
            val destinationDriveId = request.arguments["destination_drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "destination_drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DESTINATION_DRIVE_ID"
                )
            val destinationFolderId = request.arguments["destination_folder_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "destination_folder_id는 필수 매개변수입니다.",
                    code = "MISSING_DESTINATION_FOLDER_ID"
                )
            
            val copyRequest = CopyFileRequest(
                destinationDriveId = destinationDriveId,
                destinationFileId = destinationFolderId
            )
            val response = doorayClient.copyFile(driveId, fileId, copyRequest)
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = mapOf(
                        "originalDriveId" to driveId,
                        "originalFileId" to fileId,
                        "destinationDriveId" to destinationDriveId,
                        "destinationFolderId" to destinationFolderId,
                        "copiedFileId" to (response.result?.id ?: "복사됨")
                    ),
                    message = "✅ 파일을 성공적으로 복사했습니다."
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
                message = "파일 복사 중 오류가 발생했습니다: ${e.message}",
                code = "COPY_FILE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}