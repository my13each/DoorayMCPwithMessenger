package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getMemberTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_member",
        description = "두레이 멤버 ID(organizationMemberId)로 멤버 상세 정보(이름, 영문명, 사용자 코드, 이메일 등)를 조회합니다. 채널 메시지의 sender.member.organizationMemberId가 누구인지 확인할 때 사용합니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("member_id") {
                    put("type", "string")
                    put("description", "조회할 멤버 ID (organizationMemberId)")
                }
            },
            required = listOf("member_id")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getMemberHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val memberId = request.arguments["member_id"]?.jsonPrimitive?.content

            if (memberId.isNullOrBlank()) {
                val errorResponse = ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "member_id 파라미터가 필요합니다.",
                    code = "MISSING_MEMBER_ID"
                ).toErrorResponse()

                CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
            } else {
                val response = doorayClient.getMember(memberId)

                if (response.header.isSuccessful && response.result != null) {
                    val successResponse = ToolSuccessResponse(
                        data = response.result,
                        message = "👤 멤버 정보를 조회했습니다: ${response.result.name ?: memberId}"
                    )
                    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(successResponse))))
                } else {
                    val errorResponse = ToolException(
                        type = ToolException.API_ERROR,
                        message = "멤버 조회 실패: ${response.header.resultMessage}",
                        code = "GET_MEMBER_FAILED"
                    ).toErrorResponse()

                    CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
                }
            }
        } catch (e: ToolException) {
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(e.toErrorResponse()))))
        } catch (e: Exception) {
            val errorResponse = ToolException(
                type = ToolException.INTERNAL_ERROR,
                message = "멤버 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_MEMBER_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}
