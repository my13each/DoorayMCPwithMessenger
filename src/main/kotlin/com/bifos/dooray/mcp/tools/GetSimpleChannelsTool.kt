package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.SimpleChannelListResponseData
import com.bifos.dooray.mcp.types.ToolSuccessResponse
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun getSimpleChannelsTool(): Tool {
    return Tool(
        name = "dooray_messenger_get_simple_channels",
        description = "두레이 메신저에서 간단한 채널 목록을 조회합니다. 채널 검색용으로 ID, 제목, 타입, 상태, 업데이트 날짜, 참가자 수만 포함하여 모든 채널을 안전하게 조회할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("page", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("페이지 번호 (0부터 시작, 기본값: 0)"))
                    put("minimum", JsonPrimitive(0))
                })
                put("size", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("페이지당 채널 수 (기본값: 모든 채널)"))
                    put("minimum", JsonPrimitive(1))
                    put("maximum", JsonPrimitive(1000))
                })
                put("recentMonths", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("선택사항: 최근 N개월 내 업데이트된 채널만 필터링 (예: 3개월=3, 기본값: 모든 채널)"))
                    put("minimum", JsonPrimitive(1))
                    put("maximum", JsonPrimitive(12))
                })
            }
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getSimpleChannelsHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            // 입력 파라미터 파싱
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull()
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull()
            val recentMonths = request.arguments["recentMonths"]?.jsonPrimitive?.content?.toIntOrNull()

            val response = doorayClient.getSimpleChannels(
                page = page,
                size = size,
                recentMonths = recentMonths
            )

            if (response.header.isSuccessful) {
                val data = SimpleChannelListResponseData(
                    channels = response.result,
                    totalCount = response.totalCount ?: response.result.size
                )
                val filterMessage = when {
                    recentMonths != null -> " (최근 ${recentMonths}개월 필터링 적용)"
                    size != null -> " (페이지 크기: $size)"
                    else -> " (모든 채널)"
                }
                val successResponse = ToolSuccessResponse(
                    data = data,
                    message = "간단한 채널 목록 조회가 완료되었습니다$filterMessage. 상세 정보는 dooray_messenger_get_channel으로 개별 조회 가능합니다."
                )
                CallToolResult(
                    content = listOf(TextContent(JsonUtils.toJsonString(successResponse)))
                )
            } else {
                val errorResponse = ToolException(
                    type = ToolException.API_ERROR,
                    message = "간단한 채널 목록 조회 실패: ${response.header.resultMessage}",
                    code = "GET_SIMPLE_CHANNELS_FAILED"
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
                message = "간단한 채널 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_SIMPLE_CHANNELS_ERROR"
            ).toErrorResponse()
            
            CallToolResult(
                content = listOf(TextContent(JsonUtils.toJsonString(errorResponse)))
            )
        }
    }
}