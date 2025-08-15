package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun getChannelTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_channel",
        description = "두레이 메신저에서 특정 채널의 상세 정보를 조회합니다. 채널 ID를 통해 해당 채널의 모든 멤버, 설정 등 상세 정보를 확인할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("channelId", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("조회할 채널의 ID"))
                })
            },
            required = listOf("channelId")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getChannelHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            // 입력 파라미터 파싱
            val channelId = request.arguments["channelId"]?.jsonPrimitive?.content

            when {
                channelId.isNullOrBlank() -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "channelId 파라미터가 필요합니다.",
                        code = "MISSING_CHANNEL_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val channel = doorayClient.getChannel(channelId)

                    if (channel != null) {
                        val successResponse = ToolSuccessResponse(
                            data = channel,
                            message = "채널 상세 정보 조회가 완료되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "채널을 찾을 수 없습니다. 채널 ID를 확인해주세요: $channelId",
                            code = "CHANNEL_NOT_FOUND"
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
                message = "채널 정보 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_CHANNEL_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}