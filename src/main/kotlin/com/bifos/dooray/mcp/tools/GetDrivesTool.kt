package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DriveListResponseData
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject

fun getDrivesTool(): Tool {
    return Tool(
        name = "dooray_drive_list",
        description = "Dooray에서 접근 가능한 드라이브 목록을 조회합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject { }
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
                val responseData = DriveListResponseData(
                    drives = response.result,
                    totalCount = response.totalCount ?: response.result.size
                )
                
                CallToolResult(
                    content = listOf(
                        JsonUtils.createJsonContent(responseData)
                    ),
                    isError = false
                )
            } else {
                throw ToolException(
                    message = "드라이브 목록 조회 실패: ${response.header.resultMessage}",
                    code = "GET_DRIVES_ERROR"
                )
            }
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    JsonUtils.createTextContent("드라이브 목록 조회 중 오류가 발생했습니다: ${e.message}")
                ),
                isError = true
            )
        }
    }
}