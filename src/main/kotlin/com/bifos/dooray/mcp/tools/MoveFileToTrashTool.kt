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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add

fun moveFileToTrashTool(): Tool {
    return Tool(
        name = "dooray_drive_move_to_trash",
        description = "드라이브 파일을 휴지통으로 이동합니다. 휴지통으로 이동된 파일은 나중에 복원하거나 영구 삭제할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
putJsonObject("drive_id") {
                    put("type", "string")
                    put("description", "드라이브 ID")
                }
                putJsonObject("file_id") {
                    put("type", "string")
                    put("description", "휴지통으로 이동할 파일 ID")
                }
                }

                putJsonArray("required") {
                    add("drive_id")
                    add("file_id")
                }
                put("additionalProperties", false)
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun moveFileToTrashHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
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
            
            val response = doorayClient.moveFileToTrash(driveId, fileId)
            
            if (response.header.isSuccessful && response.result != null) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 파일을 휴지통으로 성공적으로 이동했습니다."
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
                message = "파일을 휴지통으로 이동하는 중 오류가 발생했습니다: ${e.message}",
                code = "MOVE_FILE_TO_TRASH_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}