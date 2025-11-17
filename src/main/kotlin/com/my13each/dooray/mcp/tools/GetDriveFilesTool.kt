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
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun getDriveFilesTool(): Tool {
    return Tool(
        name = "dooray_drive_list_files",
        description = """
            특정 드라이브의 파일 목록을 조회합니다.
            폴더/파일 필터링, 서브타입(root, trash 등) 필터링이 가능합니다.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("drive_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("드라이브 ID"))
                }
                putJsonObject("type") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("파일 타입 필터: folder(폴더만) | file(파일만). 지정하지 않으면 모두 조회"))
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("folder"))
                        add(JsonPrimitive("file"))
                    })
                }
                putJsonObject("sub_types") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("서브타입 필터 (콤마 구분): root,trash,users (폴더) | etc,doc,photo,movie,music,zip (파일)"))
                }
                putJsonObject("parent_id") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("상위 폴더 ID (선택사항, 루트는 null)"))
                }
                putJsonObject("page") {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("페이지 번호 (기본값: 0)"))
                    put("default", JsonPrimitive(0))
                }
                putJsonObject("size") {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("페이지당 항목 수 (기본값: 50)"))
                    put("default", JsonPrimitive(50))
                }
            },
            required = emptyList()
        ),
        outputSchema = null,
        annotations = null
    )
}

fun getDriveFilesHandler(doorayClient: DoorayClient): suspend (CallToolRequest) -> CallToolResult {
    return { request ->
        try {
            val driveId = request.arguments["drive_id"]?.jsonPrimitive?.content
                ?: throw ToolException(
                    type = ToolException.PARAMETER_MISSING,
                    message = "drive_id는 필수 매개변수입니다.",
                    code = "MISSING_DRIVE_ID"
                )
            val type = request.arguments["type"]?.jsonPrimitive?.content
            val subTypes = request.arguments["sub_types"]?.jsonPrimitive?.content
            val parentId = request.arguments["parent_id"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            val page = request.arguments["page"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val size = request.arguments["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50

            val response = doorayClient.getDriveFiles(driveId, type, subTypes, parentId, page, size)

            if (response.header.isSuccessful) {
                val filterInfo = buildList {
                    type?.let { add("타입=$it") }
                    subTypes?.let { add("서브타입=$it") }
                    parentId?.let { add("상위폴더=$it") }
                }.joinToString(", ")

                val message = if (filterInfo.isNotEmpty()) {
                    "✅ 드라이브 파일 목록을 성공적으로 조회했습니다 (필터: $filterInfo, 총 ${response.result.size}개)"
                } else {
                    "✅ 드라이브 파일 목록을 성공적으로 조회했습니다 (총 ${response.result.size}개)"
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
                message = "드라이브 파일 목록 조회 중 오류가 발생했습니다: ${e.message}",
                code = "GET_DRIVE_FILES_ERROR"
            ).toErrorResponse()

            CallToolResult(content = listOf(TextContent(JsonUtils.toJsonString(errorResponse))))
        }
    }
}