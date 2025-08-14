package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ChannelListResponseData
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject

fun getChannelsTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_channels",
        description = "두레이 메신저에서 접근 가능한 채널 목록을 조회합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                // 이 도구는 매개변수가 필요하지 않습니다
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getChannelsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val response = doorayClient.getChannels()

            if (response.header.isSuccessful) {
                val data = ChannelListResponseData(
                    channels = response.result,
                    totalCount = response.totalCount ?: response.result.size
                )
                val successResponse = ToolSuccessResponse(
                    data = data,
                    message = "채널 목록 조회가 완료되었습니다."
                )
                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = "채널 목록 조회 실패: ${response.header.resultMessage}",
                    code = "GET_CHANNELS_FAILED"
                ).toErrorResponse()
                
                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                )
            }
        } catch (e: ToolException) {
            val errorResponse = e.toErrorResponse()
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "채널 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_CHANNELS_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}