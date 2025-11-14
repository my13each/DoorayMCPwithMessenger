package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.DirectMessageRequest
import com.bifos.dooray.mcp.types.DirectMessageResponseData
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

fun sendDirectMessageTool(): Tool {
    return Tool(
        name = "dooray_messenger_send_direct_message",
        description = "두레이에서 특정 멤버에게 1:1 다이렉트 메시지를 전송합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("organization_member_id") {
                    put("type", "string")
                    put("description", "메시지를 받을 멤버의 조직 멤버 ID (dooray_messenger_search_members로 조회 가능)")
                }
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "전송할 메시지 내용")
                }
            },
            required = listOf("organization_member_id", "text")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun sendDirectMessageHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val organizationMemberId = request.arguments["organization_member_id"]?.jsonPrimitive?.content
            val text = request.arguments["text"]?.jsonPrimitive?.content

            when {
                organizationMemberId == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "organization_member_id 파라미터가 필요합니다.",
                        code = "MISSING_ORGANIZATION_MEMBER_ID"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                text == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "text 파라미터가 필요합니다.",
                        code = "MISSING_TEXT"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val directMessageRequest = DirectMessageRequest(
                        text = text,
                        organizationMemberId = organizationMemberId
                    )

                    val response = doorayClient.sendDirectMessage(directMessageRequest)

                    if (response.header.isSuccessful) {
                        val data = DirectMessageResponseData(
                            recipientId = organizationMemberId,
                            sentMessage = text,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "다이렉트 메시지가 성공적으로 전송되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "다이렉트 메시지 전송 실패: ${response.header.resultMessage}",
                            code = "SEND_DIRECT_MESSAGE_FAILED"
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
                message = "다이렉트 메시지 전송 중 오류가 발생했습니다: ${e.message}",
                code = "SEND_DIRECT_MESSAGE_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}