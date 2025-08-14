package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.MemberSearchResponseData
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

fun searchMembersTool(): Tool {
    return Tool(
        name = "dooray_messenger_search_members",
        description = "두레이 조직의 멤버를 검색합니다. 이름, 이메일, 사용자 코드 등으로 검색할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("name") {
                    put("type", "string")
                    put("description", "검색할 멤버 이름")
                }
                putJsonObject("email") {
                    put("type", "string")
                    put("description", "검색할 멤버 이메일 주소")
                }
                putJsonObject("user_code") {
                    put("type", "string")
                    put("description", "검색할 사용자 코드")
                }
                putJsonObject("page") {
                    put("type", "integer")
                    put("description", "페이지 번호 (기본값: 0)")
                    put("default", 0)
                }
                putJsonObject("size") {
                    put("type", "integer")
                    put("description", "페이지 크기 (기본값: 20, 최대: 100)")
                    put("default", 20)
                }
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun searchMembersHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val name = request.arguments["name"]?.jsonPrimitive?.content
            val email = request.arguments["email"]?.jsonPrimitive?.content
            val userCode = request.arguments["user_code"]?.jsonPrimitive?.content
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 20

            val emailList = email?.let { listOf(it) }

            val response = doorayClient.searchMembers(
                name = name,
                externalEmailAddresses = emailList,
                userCode = userCode,
                page = page,
                size = size
            )

            if (response.header.isSuccessful) {
                val data = MemberSearchResponseData(
                    members = response.result,
                    totalCount = response.totalCount ?: response.result.size,
                    currentPage = page,
                    pageSize = size
                )
                val successResponse = ToolSuccessResponse(
                    data = data,
                    message = "멤버 검색이 완료되었습니다."
                )
                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = "멤버 검색 실패: ${response.header.resultMessage}",
                    code = "SEARCH_MEMBERS_FAILED"
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
                message = "멤버 검색 중 오류가 발생했습니다: ${e.message}",
                code = "SEARCH_MEMBERS_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}