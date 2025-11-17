package com.my13each.dooray.mcp.tools

import com.my13each.dooray.mcp.client.DoorayClient
import com.my13each.dooray.mcp.exception.ToolException
import com.my13each.dooray.mcp.types.ToolSuccessResponse
import com.my13each.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

fun getDrivesTool(): Tool {
    return Tool(
        name = "dooray_drive_list",
        description = """
            Dooray에서 접근 가능한 드라이브 목록을 조회합니다.
            개인 드라이브, 프로젝트 드라이브를 필터링하여 조회할 수 있습니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("project_id", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("특정 프로젝트의 드라이브만 조회 (선택사항)"))
                })
                put("type", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("드라이브 타입: private(개인 드라이브) | project(프로젝트 드라이브). 기본값: 모두 조회"))
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("private"))
                        add(JsonPrimitive("project"))
                    })
                })
                put("scope", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("프로젝트 범위 (type이 project인 경우): private(일반 프로젝트) | public(공개 프로젝트). 기본값: private"))
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("private"))
                        add(JsonPrimitive("public"))
                    })
                })
                put("state", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("프로젝트 상태: active(활성) | archived(보관됨) | deleted(삭제됨). 기본값: active"))
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("active"))
                        add(JsonPrimitive("archived"))
                        add(JsonPrimitive("deleted"))
                    })
                })
            },
            required = emptyList()
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDrivesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val projectId = request.arguments["project_id"]?.jsonPrimitive?.content
            val type = request.arguments["type"]?.jsonPrimitive?.content
            val scope = request.arguments["scope"]?.jsonPrimitive?.content
            val state = request.arguments["state"]?.jsonPrimitive?.content

            val response = doorayClient.getDrives(projectId, type, scope, state)

            if (response.header.isSuccessful) {
                val filterInfo = buildList {
                    projectId?.let { add("프로젝트ID=$it") }
                    type?.let { add("타입=$it") }
                    scope?.let { add("범위=$it") }
                    state?.let { add("상태=$it") }
                }.joinToString(", ")

                val message = if (filterInfo.isNotEmpty()) {
                    "✅ 드라이브 목록을 성공적으로 조회했습니다 (필터: $filterInfo, 총 ${response.result.size}개)"
                } else {
                    "✅ 드라이브 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
                }

                val successResponse = ToolSuccessResponse(
                    data = response.result,
                    message = message
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