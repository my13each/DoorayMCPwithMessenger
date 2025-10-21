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
import java.util.Base64

fun downloadFileTool(): Tool {
    return Tool(
        name = "dooray_drive_download_file",
        description = "드라이브에서 파일을 다운로드합니다. 파일 내용을 Base64로 인코딩하여 반환합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "다운로드할 파일 ID")
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun downloadFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
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
            
            val fileContent = doorayClient.downloadFile(driveId, fileId)
            val base64Content = Base64.getEncoder().encodeToString(fileContent.toByteArray())
            
            val successResponse = ToolSuccessResponse(
                data = mapOf(
                    "driveId" to driveId,
                    "fileId" to fileId,
                    "base64Content" to base64Content,
                    "size" to fileContent.length
                ),
                message = "✅ 파일을 성공적으로 다운로드했습니다."
            )
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
            )
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "파일 다운로드 중 오류가 발생했습니다: ${e.message}",
                code = "DOWNLOAD_FILE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}