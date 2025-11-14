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
import kotlinx.serialization.json.putJsonArray

fun uploadFileTool(): Tool {
    return Tool(
        name = "dooray_drive_upload_file",
        description = """
            DoorayドライブにBase64エンコードされたファイルをアップロードします。

            ⚠️ **使用制限**:
            - 小さなファイル（10KB未満推奨）専用
            - 大きなファイルはClaudeのメッセージ長制限（約200K文字）に達します

            📌 **推奨事項**:
            - 優先的に `dooray_drive_upload_file_from_path` を使用してください
            - このツールは `dooray_drive_upload_file_from_path` が失敗した場合のフォールバック用です
            - ファイルパスが利用できない場合や、既にBase64データがある場合に使用
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("file_name") {
                    put("type", "string")
                    put("description", "파일명 (확장자 포함)")
                }
                putJsonObject("base64_content") {
                    put("type", "string")
                    put("description", "Base64로 인코딩된 파일 내용")
                }
                putJsonObject("parent_id") {
                    put("type", "string")
                    put("description", "상위 폴더 ID (필수)")
                }
                putJsonObject("mime_type") {
                    put("type", "string")
                    put("description", "MIME 타입 (예: text/plain, image/jpeg, application/pdf)")
                }
            },
            required = listOf("drive_id", "file_name", "base64_content", "parent_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun uploadFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
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
            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "parent_id는 필수 매개변수입니다.",
                    code = "MISSING_PARENT_ID"
                )
            val mimeType = request.arguments["mime_type"]?.jsonPrimitive?.content
            
            val uploadRequest = Base64UploadRequest(
                fileName = fileName,
                base64Content = base64Content,
                parentId = parentId,
                mimeType = mimeType
            )
            
            val response = doorayClient.uploadFileFromBase64(driveId, uploadRequest)
            
            if (response.header.isSuccessful && response.result != null) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 파일 '${fileName}'을(를) 드라이브에 성공적으로 업로드했습니다."
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
                message = "파일 업로드 중 오류가 발생했습니다: ${e.message}",
                code = "UPLOAD_FILE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}