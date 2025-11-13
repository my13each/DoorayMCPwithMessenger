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
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getDrivesTool(): Tool {
    return Tool(
        name = "dooray_drive_list",
        description = "Dooray에서 접근 가능한 드라이브 목록을 조회합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    // 드라이브 목록 조회는 별도 파라미터가 필요하지 않음
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDrivesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val response = doorayClient.getDrives()
            
            if (response.header.isSuccessful) {
                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = "✅ 드라이브 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
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
                message = "드라이브 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_DRIVES_ERROR"
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}