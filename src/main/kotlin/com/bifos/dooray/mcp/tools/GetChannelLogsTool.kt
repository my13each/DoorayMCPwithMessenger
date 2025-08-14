package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ChannelLogsResponseData
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

fun getChannelLogsTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_channel_logs",
        description = "두레이 메신저 채널의 메시지 로그를 조회합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "조회할 채널의 ID (dooray_messenger_get_channels로 조회 가능)")
                }
                putJsonObject("page") {
                    put("type", "integer")
                    put("description", "페이지 번호 (기본값: 0)")
                    put("default", 0)
                }
                putJsonObject("size") {
                    put("type", "integer")
                    put("description", "페이지 크기 (기본값: 20)")
                    put("default", 20)
                }
                putJsonObject("order") {
                    put("type", "string")
                    put("description", "정렬 순서 (예: createdAt, updatedAt)")
                }
            },
            required = listOf("channel_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getChannelLogsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 20
            val order = request.arguments["order"]?.jsonPrimitive?.content

            when {
                channelId == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "channel_id 파라미터가 필요합니다.",
                        code = "MISSING_CHANNEL_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val response = doorayClient.getChannelLogs(
                        channelId = channelId,
                        page = page,
                        size = size,
                        order = order
                    )

                    if (response.header.isSuccessful) {
                        val data = ChannelLogsResponseData(
                            channelId = channelId,
                            messages = response.result,
                            totalCount = response.totalCount ?: response.result.size,
                            currentPage = page,
                            pageSize = size
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "채널 로그 조회가 완료되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "채널 로그 조회 실패: ${response.header.resultMessage}",
                            code = "GET_CHANNEL_LOGS_FAILED"
                        ).toErrorResponse()
                        
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                        )
                    }
                }
            }
        } catch (e: ToolException) {
            val errorResponse = e.toErrorResponse()
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "채널 로그 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_CHANNEL_LOGS_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}