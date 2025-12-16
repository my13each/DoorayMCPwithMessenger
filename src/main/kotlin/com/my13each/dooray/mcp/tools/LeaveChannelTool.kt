package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.LeaveChannelRequest
import com.my13each.dooray.mcp.types.LeaveChannelResponseData
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun leaveChannelTool(): Tool {
    return Tool(
        name = "dooray_messenger_leave_channel",
        description = "두레이 메신저 채널에서 멤버를 제거합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("channel_id") {
                    put("type", "string")
                    put("description", "멤버를 제거할 채널의 ID")
                }
                putJsonObject("member_ids") {
                    put("type", "array")
                    put("description", "제거할 멤버 ID 목록")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }
            },
            required = listOf("channel_id", "member_ids")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun leaveChannelHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val channelId = request.arguments["channel_id"]?.jsonPrimitive?.content
            val memberIds = request.arguments["member_ids"]?.jsonArray?.map { it.jsonPrimitive.content }

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
                memberIds == null || memberIds.isEmpty() -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "member_ids 파라미터가 필요하며, 비어있지 않아야 합니다.",
                        code = "MISSING_MEMBER_IDS"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val leaveChannelRequest = LeaveChannelRequest(memberIds = memberIds)

                    val response = doorayClient.leaveChannel(channelId, leaveChannelRequest)

                    if (response.header.isSuccessful) {
                        val data = LeaveChannelResponseData(
                            channelId = channelId,
                            memberIds = memberIds,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "멤버가 성공적으로 채널에서 제거되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "채널 탈퇴 실패: ${response.header.resultMessage}",
                            code = "LEAVE_CHANNEL_FAILED"
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
                message = "채널 탈퇴 중 오류가 발생했습니다: ${e.message}",
                code = "LEAVE_CHANNEL_ERROR"
            ).toErrorResponse()

            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}
