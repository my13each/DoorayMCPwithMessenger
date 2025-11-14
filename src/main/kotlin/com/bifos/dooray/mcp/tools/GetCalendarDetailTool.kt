package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayClient
import com.bifos.dooray.mcp.exception.ToolException
import com.bifos.dooray.mcp.types.*
import com.bifos.dooray.mcp.utils.JsonUtils
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun getCalendarDetailTool(): Tool {
    return Tool(
        name = "dooray_calendar_detail",
        description = "두레이에서 특정 캘린더의 상세 정보를 조회합니다. 캘린더 멤버 목록, 권한 정보, 위임 정보 등을 확인할 수 있습니다.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("calendarId") {
                    put("type", "string")
                    put("description", "조회할 캘린더 ID (dooray_calendar_list에서 확인 가능)")
                }
            },
            required = listOf("calendarId")
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getCalendarDetailHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val calendarId = request.arguments["calendarId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("calendarId is required")

            val response = doorayClient.getCalendarDetail(calendarId)
            
            if (response.header.isSuccessful) {
                val calendar = response.result
                
                // 멤버 목록 포맷팅
                val membersInfo = if (calendar.calendarMemberList.isNotEmpty()) {
                    buildString {
                        appendLine("👥 **캘린더 멤버 (${calendar.calendarMemberList.size}명):**")
                        calendar.calendarMemberList.forEach { member ->
                            val roleEmoji = when (member.role) {
                                "owner" -> "👑"
                                "delegatee" -> "🤝"
                                "all" -> "🔓"
                                "read_write" -> "✏️"
                                "view" -> "👀"
                                "opaque_view" -> "👁️"
                                else -> "❔"
                            }
                            appendLine("- $roleEmoji ${member.member.organizationMemberId} (${member.role})")
                        }
                    }
                } else {
                    "👤 개인 캘린더 (멤버 공유 없음)"
                }
                
                // 내 권한 정보
                val myRoleEmoji = when (calendar.me.role) {
                    "owner" -> "👑"
                    "delegatee" -> "🤝"
                    "all" -> "🔓"
                    "read_write" -> "✏️"
                    "view" -> "👀"
                    "opaque_view" -> "👁️"
                    else -> "❔"
                }
                
                val successData = mapOf(
                    "id" to calendar.id,
                    "name" to calendar.name,
                    "type" to calendar.type,
                    "createdAt" to (calendar.createdAt ?: "정보 없음"),
                    "ownerOrganizationMemberId" to (calendar.ownerOrganizationMemberId ?: "정보 없음"),
                    "projectId" to (calendar.projectId ?: "해당없음"),
                    "memberCount" to calendar.calendarMemberList.size,
                    "myRole" to calendar.me.role,
                    "myColor" to calendar.me.color,
                    "myOrder" to calendar.me.order,
                    "isDefault" to calendar.me.default,
                    "isListed" to calendar.me.listed,
                    "isChecked" to calendar.me.checked
                )
                
                val typeDisplay = when (calendar.type) {
                    "private" -> "📱 개인 캘린더"
                    "project" -> "👥 프로젝트 캘린더"
                    "subscription" -> "📅 구독 캘린더"
                    else -> "❔ ${calendar.type}"
                }
                
                val successResponse = ToolSuccessResponse(
                    data = successData,
                    message = "📅 **${calendar.name}** 캘린더 상세 정보\n\n" +
                            "🆔 **ID:** ${calendar.id}\n" +
                            "📝 **타입:** $typeDisplay\n" +
                            "📅 **생성일:** ${calendar.createdAt ?: "정보 없음"}\n" +
                            "${if (calendar.projectId != null) "🏗️ **프로젝트 ID:** ${calendar.projectId}\n" else ""}" +
                            "\n🔐 **내 권한 정보:**\n" +
                            "- $myRoleEmoji 역할: ${calendar.me.role}\n" +
                            "- 🎨 컬러: ${calendar.me.color}\n" +
                            "- 📊 순서: ${calendar.me.order}\n" +
                            "- ${if (calendar.me.default) "⭐ 기본 캘린더" else "📋 일반 캘린더"}\n" +
                            "- ${if (calendar.me.listed) "👁️ 목록에 표시됨" else "🙈 목록에 숨겨짐"}\n" +
                            "- ${if (calendar.me.checked) "✅ 체크됨" else "☑️ 체크 해제"}\n\n" +
                            membersInfo
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
                message = "캘린더 상세 조회 중 오류가 발생했습니다: ${e.message}",
                details = e.stackTraceToString()
            ).toErrorResponse()
            
            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}