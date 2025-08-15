package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.CreateChannelRequest
import com.bifos.dooray.mcp.types.CreateChannelResponseData
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun createChannelTool(): Tool {
    return Tool(
        name = "dooray_messenger_create_channel",
        description = "두레이 메신저에서 새로운 채널을 생성합니다. (private 또는 direct 타입)",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("type") {
                    put("type", "string")
                    put("description", "채널 타입 (private 또는 direct)")
                }
                putJsonObject("title") {
                    put("type", "string")
                    put("description", "채널 제목/이름")
                }
                putJsonObject("capacity") {
                    put("type", "string")
                    put("description", "채널 참가 가능 인원수 (문자열)")
                    put("default", "100")
                }
                putJsonObject("member_ids") {
                    put("type", "array")
                    put("description", "초대할 멤버 ID 목록")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }
                putJsonObject("id_type") {
                    put("type", "string") 
                    put("description", "멤버 ID 타입 (email 또는 memberId)")
                    put("default", "memberId")
                }
            },
            required = listOf("type", "title")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun createChannelHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val type = request.arguments["type"]?.jsonPrimitive?.content
            val title = request.arguments["title"]?.jsonPrimitive?.content
            val capacity = request.arguments["capacity"]?.jsonPrimitive?.content
            val memberIds = request.arguments["member_ids"]?.jsonArray?.map { it.jsonPrimitive.content }
            val idType = request.arguments["id_type"]?.jsonPrimitive?.content

            when {
                type == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "type 파라미터가 필요합니다. (\"private\" 또는 \"direct\")",
                        code = "MISSING_TYPE"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                title == null -> {
                    val errorResponse = ToolException(
                        type = ToolException.PARAMETER_MISSING,
                        message = "title 파라미터가 필요합니다.",
                        code = "MISSING_TITLE"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                type !in listOf("private", "direct") -> {
                    val errorResponse = ToolException(
                        type = ToolException.VALIDATION_ERROR,
                        message = "type은 \"private\" 또는 \"direct\"여야 합니다.",
                        code = "INVALID_TYPE"
                    ).toErrorResponse()

                    CallToolResult(
                        content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
                    )
                }
                else -> {
                    val createChannelRequest = CreateChannelRequest(
                        type = type,
                        capacity = capacity,
                        memberIds = memberIds,
                        title = title
                    )

                    val response = doorayClient.createChannel(createChannelRequest, idType)

                    if (response.header.isSuccessful && response.result != null) {
                        val data = CreateChannelResponseData(
                            channelId = response.result.id,
                            channelTitle = title,
                            channelType = type,
                            memberCount = memberIds?.size ?: 0,
                            timestamp = System.currentTimeMillis()
                        )
                        val successResponse = ToolSuccessResponse(
                            data = data,
                            message = "채널이 성공적으로 생성되었습니다."
                        )
                        CallToolResult(
                            content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                        )
                    } else {
                        val errorResponse = ToolException(
                            type = ToolException.API_ERROR,
                            message = "채널 생성 실패: ${response.header.resultMessage}",
                            code = "CREATE_CHANNEL_FAILED"
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
                message = "채널 생성 중 오류가 발생했습니다: ${e.message}",
                code = "CREATE_CHANNEL_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}