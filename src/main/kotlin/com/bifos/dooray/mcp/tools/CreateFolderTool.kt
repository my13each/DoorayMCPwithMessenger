package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CreateFolderRequest
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

fun createFolderTool(): Tool {
    return Tool(
        name = "dooray_drive_create_folder",
        description = "드라이브에 새 폴더를 생성합니다. 지정된 상위 폴더 하위에 새 폴더를 생성합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {

                putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("parent_folder_id") {
                    put("type", "string")
                    put("description", "상위 폴더 ID")
                }
                putJsonObject("folder_name") {
                    put("type", "string")
                    put("description", "생성할 폴더명")
                }
                }
                putJsonArray("required") {
                    add("drive_id")
                    add("parent_folder_id")
                    add("folder_name")
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun createFolderHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )
            val parentFolderId = request.arguments["parent_folder_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "parent_folder_id는 필수 매개변수입니다.",
                    code = "MISSING_PARENT_FOLDER_ID"
                )
            val folderName = request.arguments["folder_name"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "folder_name은 필수 매개변수입니다.",
                    code = "MISSING_FOLDER_NAME"
                )
            
            val createRequest = CreateFolderRequest(name = folderName)
            val response = doorayClient.createFolder(driveId, parentFolderId, createRequest)
            
            if (response.header.isSuccessful && response.result != null) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 폴더 '${folderName}'을(를) 성공적으로 생성했습니다."
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
                message = "폴더 생성 중 오류가 발생했습니다: ${e.message}",
                code = "CREATE_FOLDER_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}