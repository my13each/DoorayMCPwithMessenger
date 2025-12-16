package com.my13each.dooray.mcp

import com.my13each.dooray.mcp.client.DoorayHttpClient
import com.my13each.dooray.mcp.constants.EnvVariableConst.DOORAY_API_KEY
import com.my13each.dooray.mcp.constants.EnvVariableConst.DOORAY_BASE_URL
import com.my13each.dooray.mcp.constants.EnvVariableConst.DOORAY_ENABLED_CATEGORIES
import com.my13each.dooray.mcp.constants.ToolCategory
import com.my13each.dooray.mcp.constants.VersionConst
import com.my13each.dooray.mcp.tools.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.slf4j.LoggerFactory

class DoorayMcpServer {

    private val log = LoggerFactory.getLogger(DoorayMcpServer::class.java)

    fun initServer() {
        log.info("Dooray MCP Server starting...")

        val env = getEnv()

        log.info("DOORAY_API_KEY, DOORAY_BASE_URL found, initializing HTTP client...")
        val doorayHttpClient =
            DoorayHttpClient(
                baseUrl = env[DOORAY_BASE_URL]!!,
                doorayApiKey = env[DOORAY_API_KEY]!!
            )

        val server =
            Server(
                Implementation(
                    name = "dooray-mcp-server",
                    version = VersionConst.VERSION
                ),
                ServerOptions(
                    capabilities =
                        ServerCapabilities(
                            tools =
                                ServerCapabilities.Tools(
                                    listChanged = true
                                )
                        )
                )
            )

        registerTool(server, doorayHttpClient)

        // Create a transport using standard IO for server communication
        val transport =
            StdioServerTransport(System.`in`.asInput(), System.out.asSink().buffered())

        log.info("Starting MCP server on STDIO transport...")

        runBlocking {
            server.connect(transport)
            log.info("MCP server connected and ready!")

            val done = Job()
            server.onClose {
                log.info("MCP server closing...")
                done.complete()
            }
            done.join()
        }
    }

    fun getEnv(): Map<String, String> {
        val baseUrl =
            System.getenv(DOORAY_BASE_URL)
                ?: throw IllegalArgumentException("DOORAY_BASE_URL is required.")
        val apiKey =
            System.getenv(DOORAY_API_KEY)
                ?: throw IllegalArgumentException("DOORAY_API_KEY is required.")

        return mapOf(
            DOORAY_BASE_URL to baseUrl,
            DOORAY_API_KEY to apiKey,
        )
    }

    fun registerTool(server: Server, doorayHttpClient: DoorayHttpClient) {
        log.info("Adding tools...")

        // 환경변수에서 활성화된 카테고리 파싱
        val enabledCategoriesEnv = System.getenv(DOORAY_ENABLED_CATEGORIES)
        val enabledCategories = ToolCategory.parseEnabledCategories(enabledCategoriesEnv)

        if (enabledCategoriesEnv.isNullOrBlank()) {
            log.info("DOORAY_ENABLED_CATEGORIES not set. All categories will be enabled.")
        } else {
            log.info("DOORAY_ENABLED_CATEGORIES: $enabledCategoriesEnv")
            log.info("Enabled categories: ${enabledCategories.joinToString(", ")}")
        }

        var toolCount = 0

        fun addTool(category: ToolCategory, tool: Tool, handler: suspend (CallToolRequest) -> CallToolResult) {
            if (category !in enabledCategories) {
                // 카테고리가 활성화되지 않았으면 도구를 등록하지 않음
                return
            }

            server.addTool(
                name = tool.name,
                description = tool.description ?: "",
                inputSchema = tool.inputSchema,
                handler = handler
            )
            toolCount++
        }

        // ============ Wiki 관련 도구들 (5개) ============

        // 1. 위키 프로젝트 목록 조회
        addTool(ToolCategory.WIKI, getWikisTool(), getWikisHandler(doorayHttpClient))

        // 2. 위키 페이지 목록 조회
        addTool(ToolCategory.WIKI, getWikiPagesTool(), getWikiPagesHandler(doorayHttpClient))

        // 3. 위키 페이지 상세 조회
        addTool(ToolCategory.WIKI, getWikiPageTool(), getWikiPageHandler(doorayHttpClient))

        // 4. 위키 페이지 생성
        addTool(ToolCategory.WIKI, createWikiPageTool(), createWikiPageHandler(doorayHttpClient))

        // 5. 위키 페이지 수정
        addTool(ToolCategory.WIKI, updateWikiPageTool(), updateWikiPageHandler(doorayHttpClient))

        // ============ 프로젝트 업무 및 댓글 관련 도구들 (11개) ============

        // 6. 프로젝트 업무 목록 조회
        addTool(ToolCategory.PROJECT, getProjectPostsTool(), getProjectPostsHandler(doorayHttpClient))

        // 7. 프로젝트 업무 상세 조회
        addTool(ToolCategory.PROJECT, getProjectPostTool(), getProjectPostHandler(doorayHttpClient))

        // 8. 프로젝트 업무 생성
        addTool(ToolCategory.PROJECT, createProjectPostTool(), createProjectPostHandler(doorayHttpClient))

        // 9. 프로젝트 업무 상태 변경
        addTool(
            ToolCategory.PROJECT,
            setProjectPostWorkflowTool(),
            setProjectPostWorkflowHandler(doorayHttpClient)
        )

        // 10. 프로젝트 업무 완료 처리
        addTool(ToolCategory.PROJECT, setProjectPostDoneTool(), setProjectPostDoneHandler(doorayHttpClient))

        // 11. 프로젝트 목록 조회
        addTool(ToolCategory.PROJECT, getProjectsTool(), getProjectsHandler(doorayHttpClient))

        // 12. 프로젝트 업무 수정
        addTool(ToolCategory.PROJECT, updateProjectPostTool(), updateProjectPostHandler(doorayHttpClient))

        // 13. 업무 댓글 생성
        addTool(ToolCategory.PROJECT, createPostCommentTool(), createPostCommentHandler(doorayHttpClient))

        // 14. 업무 댓글 목록 조회
        addTool(ToolCategory.PROJECT, getPostCommentsTool(), getPostCommentsHandler(doorayHttpClient))

        // 15. 업무 댓글 수정
        addTool(ToolCategory.PROJECT, updatePostCommentTool(), updatePostCommentHandler(doorayHttpClient))

        // 16. 업무 댓글 삭제
        addTool(ToolCategory.PROJECT, deletePostCommentTool(), deletePostCommentHandler(doorayHttpClient))

        // ============ 메신저 관련 도구들 (7개) ============

        // 17. 멤버 검색
        addTool(ToolCategory.MESSENGER, searchMembersTool(), searchMembersHandler(doorayHttpClient))

        // 18. 다이렉트 메시지 전송
        addTool(ToolCategory.MESSENGER, sendDirectMessageTool(), sendDirectMessageHandler(doorayHttpClient))

        // 19. 채널 목록 조회
        addTool(ToolCategory.MESSENGER, getChannelsTool(), getChannelsHandler(doorayHttpClient))

        // 20. 간단한 채널 목록 조회 (검색용)
        addTool(ToolCategory.MESSENGER, getSimpleChannelsTool(), getSimpleChannelsHandler(doorayHttpClient))

        // 21. 특정 채널 상세 조회
        addTool(ToolCategory.MESSENGER, getChannelTool(), getChannelHandler(doorayHttpClient))

        // ⚠️ 채널 로그 조회는 Dooray API에서 지원하지 않음 (보안상 제한)
        // 22. 채널 메시지 전송
        addTool(ToolCategory.MESSENGER, sendChannelMessageTool(), sendChannelMessageHandler(doorayHttpClient))

        // 23. 채널 생성
        addTool(ToolCategory.MESSENGER, createChannelTool(), createChannelHandler(doorayHttpClient))

        // 24. 채널 가입 (멤버 추가)
        addTool(ToolCategory.MESSENGER, joinChannelTool(), joinChannelHandler(doorayHttpClient))

        // 25. 채널 탈퇴 (멤버 제거)
        addTool(ToolCategory.MESSENGER, leaveChannelTool(), leaveChannelHandler(doorayHttpClient))

        // 26. 스레드 생성 및 메시지 전송
        addTool(ToolCategory.MESSENGER, createThreadTool(), createThreadHandler(doorayHttpClient))

        // 27. 메시지 수정
        addTool(ToolCategory.MESSENGER, updateMessageTool(), updateMessageHandler(doorayHttpClient))

        // 28. 메시지 삭제
        addTool(ToolCategory.MESSENGER, deleteMessageTool(), deleteMessageHandler(doorayHttpClient))

        // ============ 캘린더 관련 도구들 (5개) ============

        // 29. 캘린더 목록 조회
        addTool(ToolCategory.CALENDAR, getCalendarsTool(), getCalendarsHandler(doorayHttpClient))

        // 30. 캘린더 상세 조회
        addTool(ToolCategory.CALENDAR, getCalendarDetailTool(), getCalendarDetailHandler(doorayHttpClient))

        // 31. 캘린더 일정 조회 (기간별)
        addTool(ToolCategory.CALENDAR, getCalendarEventsTool(), getCalendarEventsHandler(doorayHttpClient))

        // 32. 캘린더 일정 상세 조회
        addTool(ToolCategory.CALENDAR, getCalendarEventDetailTool(), getCalendarEventDetailHandler(doorayHttpClient))

        // 33. 캘린더 일정 등록
        addTool(ToolCategory.CALENDAR, createCalendarEventTool(), createCalendarEventHandler(doorayHttpClient))

        // ============ Drive 및 공유링크 관련 도구들 (20개) ============

        // 34. 드라이브 목록 조회
        addTool(ToolCategory.DRIVE, getDrivesTool(), getDrivesHandler(doorayHttpClient))

        // 35. 드라이브 상세 조회
        addTool(ToolCategory.DRIVE, getDriveDetailTool(), getDriveDetailHandler(doorayHttpClient))

        // 36. 드라이브 파일 목록 조회
        addTool(ToolCategory.DRIVE, getDriveFilesTool(), getDriveFilesHandler(doorayHttpClient))

        // 37. 드라이브 변경사항 조회
        addTool(ToolCategory.DRIVE, getDriveChangesTool(), getDriveChangesHandler(doorayHttpClient))

        // 38. 파일 업로드 (パスから) - 推奨方法 (優先使用)
        addTool(ToolCategory.DRIVE, uploadFileFromPathTool(), uploadFileFromPathHandler(doorayHttpClient))

        // 39. 파일 업로드 (Base64) - フォールバック用
        // dooray_drive_upload_file_from_path が失敗した場合（ファイルが見つからない等）の
        // バックアップ方法として使用。小さなファイル（10KB未満推奨）に適しています。
        // ⚠️ 大きなファイルはClaudeのメッセージ長制限（200K文字）に達する可能性があります
        addTool(ToolCategory.DRIVE, uploadFileTool(), uploadFileHandler(doorayHttpClient))

        // 40. 파일 다운로드
        addTool(ToolCategory.DRIVE, downloadFileTool(), downloadFileHandler(doorayHttpClient))

        // 41. 파일 메타정보 조회
        addTool(ToolCategory.DRIVE, getFileMetadataTool(), getFileMetadataHandler(doorayHttpClient))

        // 42. 파일 이름 변경
        addTool(ToolCategory.DRIVE, renameFileTool(), renameFileHandler(doorayHttpClient))

        // 43. 파일 업데이트
        addTool(ToolCategory.DRIVE, updateFileTool(), updateFileHandler(doorayHttpClient))

        // 44. 파일을 휴지통으로 이동
        addTool(ToolCategory.DRIVE, moveFileToTrashTool(), moveFileToTrashHandler(doorayHttpClient))

        // 45. 파일 영구 삭제
        addTool(ToolCategory.DRIVE, deleteFileTool(), deleteFileHandler(doorayHttpClient))

        // 46. 폴더 생성
        addTool(ToolCategory.DRIVE, createFolderTool(), createFolderHandler(doorayHttpClient))

        // 47. 파일 복사
        addTool(ToolCategory.DRIVE, copyFileTool(), copyFileHandler(doorayHttpClient))

        // 48. 파일 이동
        addTool(ToolCategory.DRIVE, moveFileTool(), moveFileHandler(doorayHttpClient))

        // 49. 공유 링크 생성
        addTool(ToolCategory.DRIVE, createSharedLinkTool(), createSharedLinkHandler(doorayHttpClient))

        // 50. 공유 링크 목록 조회
        addTool(ToolCategory.DRIVE, getSharedLinksTool(), getSharedLinksHandler(doorayHttpClient))

        // 51. 공유 링크 상세 조회
        addTool(ToolCategory.DRIVE, getSharedLinkDetailTool(), getSharedLinkDetailHandler(doorayHttpClient))

        // 52. 공유 링크 수정
        addTool(ToolCategory.DRIVE, updateSharedLinkTool(), updateSharedLinkHandler(doorayHttpClient))

        // 53. 공유 링크 삭제
        addTool(ToolCategory.DRIVE, deleteSharedLinkTool(), deleteSharedLinkHandler(doorayHttpClient))

        // 도구 개수: 53개 (Wiki 5 + Project 11 + Messenger 12 + Calendar 5 + Drive 20)
        // 카테고리: WIKI, PROJECT, MESSENGER, CALENDAR, DRIVE

        log.info("Successfully added $toolCount tools to MCP server (enabled categories: ${enabledCategories.joinToString(", ")})")
    }
}
