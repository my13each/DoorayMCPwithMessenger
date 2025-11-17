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
import kotlinx.serialization.json.putJsonObject

fun renameFileTool(): Tool {
    return Tool(
        name = "dooray_drive_rename_file",
        description = """
            드라이브의 파일 또는 폴더 이름을 변경합니다.
            파일의 확장자를 변경하거나 폴더 이름을 수정할 수 있습니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("드라이브 ID"))
                }
                putJsonObject("file_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("변경할 파일/폴더 ID"))
                }
                putJsonObject("new_name") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("새로운 파일/폴더 이름 (확장자 포함)"))
                }
            },
            required = listOf("drive_id", "file_id", "new_name")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun renameFileHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
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
            val newName = request.arguments["new_name"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "new_name은 필수 매개변수입니다.",
                    code = "MISSING_NEW_NAME"
                )

            val response = doorayClient.renameFile(driveId, fileId, newName)

            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = mapOf(
                        "driveId" to driveId,
                        "fileId" to fileId,
                        "newName" to newName
                    ),
                    message = "✅ 파일 이름을 '$newName'(으)로 성공적으로 변경했습니다."
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
                message = "파일 이름 변경 중 오류가 발생했습니다: ${e.message}",
                code = "RENAME_FILE_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
