package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.Base64UploadRequest
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

fun updateFileTool(): Tool {
    return Tool(
        name = "dooray_drive_update_file",
        description = "드라이브의 기존 파일을 새로운 버전으로 업데이트합니다. 파일 내용은 Base64로 인코딩되어야 합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "업데이트할 파일 ID")
                }
                putJsonObject("file_name") {
                    put("type", "string")
                    put("description", "파일명 (확장자 포함)")
                }
                putJsonObject("base64_content") {
                    put("type", "string")
                    put("description", "Base64로 인코딩된 새 파일 내용")
                }
                putJsonObject("mime_type") {
                    put("type", "string")
                    put("description", "MIME 타입 (예: text/plain, image/jpeg, application/pdf)")
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun updateFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
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
            val fileName = request.arguments["file_name"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "file_name은 필수 매개변수입니다.",
                    code = "MISSING_FILE_NAME"
                )
            val base64Content = request.arguments["base64_content"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "base64_content는 필수 매개변수입니다.",
                    code = "MISSING_BASE64_CONTENT"
                )
            val mimeType = request.arguments["mime_type"]?.jsonPrimitive?.content
            
            val updateRequest = Base64UploadRequest(
                fileName = fileName,
                base64Content = base64Content,
                parentId = "", // 업데이트에서는 parentId가 필요없지만 타입상 필요
                mimeType = mimeType
            )
            
            val response = doorayClient.updateFileFromBase64(driveId, fileId, updateRequest)
            
            if (response.header.isSuccessful && response.result != null) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 파일 '${fileName}'을(를) 성공적으로 업데이트했습니다."
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
                message = "파일 업데이트 중 오류가 발생했습니다: ${e.message}",
                code = "UPDATE_FILE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}