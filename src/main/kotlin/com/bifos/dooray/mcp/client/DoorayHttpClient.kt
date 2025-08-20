package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.exception.CustomException
import com.bifos.dooray.mcp.types.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class DoorayHttpClient(private val baseUrl: String, private val doorayApiKey: String) :
        DoorayClient {

    private val log = LoggerFactory.getLogger(DoorayHttpClient::class.java)
    private val httpClient: HttpClient

    init {
        httpClient = initHttpClient()
    }

    private fun initHttpClient(): HttpClient {
        return HttpClient {
            defaultRequest {
                url(baseUrl)
                header("Authorization", "dooray-api $doorayApiKey")
                contentType(ContentType.Application.Json)
            }

            // install content negotiation plugin for JSON serialization/deserialization
            install(ContentNegotiation) {
                json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }
                )
            }

            // HTTP 요청/응답 로깅 활성화 (SLF4J 사용, stdout 오염 방지)
            install(Logging) {
                logger =
                        object : Logger {
                            override fun log(message: String) {
                                log.debug("HTTP: $message")
                            }
                        }
                // 환경변수로 로깅 레벨 제어 (기본: NONE, 디버깅시: INFO)
                level =
                        when (System.getenv("DOORAY_HTTP_LOG_LEVEL")?.uppercase()) {
                            "ALL" -> LogLevel.ALL
                            "HEADERS" -> LogLevel.HEADERS
                            "BODY" -> LogLevel.BODY
                            "INFO" -> LogLevel.INFO
                            else -> LogLevel.NONE // 기본값: 로깅 비활성화
                        }
            }
        }
    }

    /**
     * API 호출을 공통 템플릿으로 처리합니다.
     * @param operation API 요청 설명 (로깅용)
     * @param expectedStatusCode 성공으로 간주할 HTTP 상태 코드
     * @param successMessage 성공 시 로깅할 메시지 (null이면 기본 메시지)
     * @param apiCall 실제 HTTP 호출을 수행하는 lambda
     */
    private suspend inline fun <reified T> executeApiCall(
            operation: String,
            expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
            successMessage: String? = null,
            crossinline apiCall: suspend () -> HttpResponse
    ): T {
        try {
            log.info("🔗 API 요청: $operation")
            val response = apiCall()
            log.info("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                expectedStatusCode -> {
                    val result = response.body<T>()
                    log.info(successMessage ?: "✅ API 호출 성공")
                    result
                }
                else -> {
                    handleErrorResponse(response)
                }
            }
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            handleGenericException(e)
        }
    }

    /** 에러 응답을 공통으로 처리합니다. */
    private suspend fun handleErrorResponse(response: HttpResponse): Nothing {
        val responseBody = response.bodyAsText()
        log.error("❌ API 오류 응답:")
        log.error("  상태 코드: ${response.status.value} ${response.status.description}")
        log.error("  응답 본문: $responseBody")

        try {
            val errorResponse = response.body<DoorayErrorResponse>()
            val errorMessage = "API 호출 실패: ${errorResponse.header.resultMessage}"
            throw CustomException(errorMessage, response.status.value)
        } catch (parseException: Exception) {
            val errorMessage = "API 응답 파싱 실패 (${response.status.value}): $responseBody"
            throw CustomException(errorMessage, response.status.value, parseException)
        }
    }

    /** 일반 예외를 공통으로 처리합니다. */
    private fun handleGenericException(e: Exception): Nothing {
        log.error("❌ 네트워크 또는 기타 오류:")
        log.error("  타입: ${e::class.simpleName}")
        log.error("  메시지: ${e.message}")
        log.error("스택 트레이스:", e)

        val errorMessage = "API 호출 중 오류 발생: ${e.message}"
        throw CustomException(errorMessage, null, e)
    }

    /** result가 null일 수 있는 API 호출을 위한 특별 처리 */
    private suspend fun executeApiCallForNullableResult(
            operation: String,
            expectedStatusCode: HttpStatusCode = HttpStatusCode.OK,
            successMessage: String,
            apiCall: suspend () -> HttpResponse
    ): DoorayApiUnitResponse {
        try {
            log.info("🔗 API 요청: $operation")
            val response = apiCall()
            log.info("📡 응답 수신: ${response.status} ${response.status.description}")

            return when (response.status) {
                expectedStatusCode -> {
                    // result가 null일 수 있는 응답을 파싱
                    val jsonResponse = response.body<DoorayApiUnitResponse>()
                    if (jsonResponse.header.isSuccessful) {
                        log.info(successMessage)
                    } else {
                        log.warn("⚠️ API 응답 에러: ${jsonResponse.header.resultMessage}")
                    }
                    jsonResponse
                }
                else -> {
                    handleErrorResponse(response)
                }
            }
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            handleGenericException(e)
        }
    }

    override suspend fun getWikis(page: Int?, size: Int?): WikiListResponse {
        return executeApiCall(operation = "GET /wiki/v1/wikis", successMessage = "✅ 위키 목록 조회 성공") {
            httpClient.get("/wiki/v1/wikis") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
            }
        }
    }

    override suspend fun getWikiPages(projectId: String): WikiPagesResponse {
        return executeApiCall(
                operation = "GET /wiki/v1/wikis/$projectId/pages",
                successMessage = "✅ 위키 페이지 목록 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages") }
    }

    override suspend fun getWikiPages(projectId: String, parentPageId: String): WikiPagesResponse {
        return executeApiCall(
                operation = "GET /wiki/v1/wikis/$projectId/pages?parentPageId=$parentPageId",
                successMessage = "✅ 자식 위키 페이지 목록 조회 성공"
        ) {
            httpClient.get("/wiki/v1/wikis/$projectId/pages") {
                parameter("parentPageId", parentPageId)
            }
        }
    }

    override suspend fun getWikiPage(projectId: String, pageId: String): WikiPageResponse {
        return executeApiCall(
                operation = "GET /wiki/v1/wikis/$projectId/pages/$pageId",
                successMessage = "✅ 위키 페이지 조회 성공"
        ) { httpClient.get("/wiki/v1/wikis/$projectId/pages/$pageId") }
    }

    override suspend fun createWikiPage(
            wikiId: String,
            request: CreateWikiPageRequest
    ): CreateWikiPageResponse {
        return executeApiCall(
                operation = "POST /wiki/v1/wikis/$wikiId/pages",
                expectedStatusCode = HttpStatusCode.Created,
                successMessage = "✅ 위키 페이지 생성 성공"
        ) { httpClient.post("/wiki/v1/wikis/$wikiId/pages") { setBody(request) } }
    }

    override suspend fun updateWikiPage(
            wikiId: String,
            pageId: String,
            request: UpdateWikiPageRequest
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
                operation = "PUT /wiki/v1/wikis/$wikiId/pages/$pageId",
                successMessage = "✅ 위키 페이지 수정 성공"
        ) { httpClient.put("/wiki/v1/wikis/$wikiId/pages/$pageId") { setBody(request) } }
    }

    // ============ 프로젝트 업무 관련 API 구현 ============

    override suspend fun createPost(
            projectId: String,
            request: CreatePostRequest
    ): CreatePostApiResponse {
        return executeApiCall(
                operation = "POST /project/v1/projects/$projectId/posts",
                expectedStatusCode = HttpStatusCode.OK,
                successMessage = "✅ 업무 생성 성공"
        ) { httpClient.post("/project/v1/projects/$projectId/posts") { setBody(request) } }
    }

    override suspend fun getPosts(
            projectId: String,
            page: Int?,
            size: Int?,
            fromMemberIds: List<String>?,
            toMemberIds: List<String>?,
            ccMemberIds: List<String>?,
            tagIds: List<String>?,
            parentPostId: String?,
            postNumber: String?,
            postWorkflowClasses: List<String>?,
            postWorkflowIds: List<String>?,
            milestoneIds: List<String>?,
            subjects: String?,
            createdAt: String?,
            updatedAt: String?,
            dueAt: String?,
            order: String?
    ): PostListResponse {
        return executeApiCall(
                operation = "GET /project/v1/projects/$projectId/posts",
                successMessage = "✅ 업무 목록 조회 성공"
        ) {
            httpClient.get("/project/v1/projects/$projectId/posts") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
                fromMemberIds?.let {
                    if (it.isNotEmpty()) parameter("fromMemberIds", it.joinToString(","))
                }
                toMemberIds?.let {
                    if (it.isNotEmpty()) parameter("toMemberIds", it.joinToString(","))
                }
                ccMemberIds?.let {
                    if (it.isNotEmpty()) parameter("ccMemberIds", it.joinToString(","))
                }
                tagIds?.let { if (it.isNotEmpty()) parameter("tagIds", it.joinToString(",")) }
                parentPostId?.let { parameter("parentPostId", it) }
                postNumber?.let { parameter("postNumber", it) }
                postWorkflowClasses?.let {
                    if (it.isNotEmpty()) parameter("postWorkflowClasses", it.joinToString(","))
                }
                postWorkflowIds?.let {
                    if (it.isNotEmpty()) parameter("postWorkflowIds", it.joinToString(","))
                }
                milestoneIds?.let {
                    if (it.isNotEmpty()) parameter("milestoneIds", it.joinToString(","))
                }
                subjects?.let { parameter("subjects", it) }
                createdAt?.let { parameter("createdAt", it) }
                updatedAt?.let { parameter("updatedAt", it) }
                dueAt?.let { parameter("dueAt", it) }
                order?.let { parameter("order", it) }
            }
        }
    }

    override suspend fun getPost(projectId: String, postId: String): PostDetailResponse {
        return executeApiCall(
                operation = "GET /project/v1/projects/$projectId/posts/$postId",
                successMessage = "✅ 업무 상세 조회 성공"
        ) { httpClient.get("/project/v1/projects/$projectId/posts/$postId") }
    }

    override suspend fun updatePost(
            projectId: String,
            postId: String,
            request: UpdatePostRequest
    ): UpdatePostResponse {
        return executeApiCallForNullableResult(
                operation = "PUT /project/v1/projects/$projectId/posts/$postId",
                successMessage = "✅ 업무 수정 성공"
        ) { httpClient.put("/project/v1/projects/$projectId/posts/$postId") { setBody(request) } }
    }

    override suspend fun updatePostUserWorkflow(
            projectId: String,
            postId: String,
            organizationMemberId: String,
            workflowId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
                operation =
                        "PUT /project/v1/projects/$projectId/posts/$postId/to/$organizationMemberId",
                successMessage = "✅ 담당자 상태 변경 성공"
        ) {
            httpClient.put(
                    "/project/v1/projects/$projectId/posts/$postId/to/$organizationMemberId"
            ) { setBody(SetWorkflowRequest(workflowId)) }
        }
    }

    override suspend fun setPostWorkflow(
            projectId: String,
            postId: String,
            workflowId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
                operation = "POST /project/v1/projects/$projectId/posts/$postId/set-workflow",
                successMessage = "✅ 업무 상태 변경 성공"
        ) {
            httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-workflow") {
                setBody(SetWorkflowRequest(workflowId))
            }
        }
    }

    override suspend fun setPostDone(projectId: String, postId: String): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
                operation = "POST /project/v1/projects/$projectId/posts/$postId/set-done",
                successMessage = "✅ 업무 완료 처리 성공"
        ) { httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-done") }
    }

    override suspend fun setPostParent(
            projectId: String,
            postId: String,
            parentPostId: String
    ): DoorayApiUnitResponse {
        return executeApiCallForNullableResult(
                operation = "POST /project/v1/projects/$projectId/posts/$postId/set-parent-post",
                successMessage = "✅ 상위 업무 설정 성공"
        ) {
            httpClient.post("/project/v1/projects/$projectId/posts/$postId/set-parent-post") {
                setBody(SetParentPostRequest(parentPostId))
            }
        }
    }

    // ============ 업무 댓글 관련 API 구현 ============

    override suspend fun createPostComment(
            projectId: String,
            postId: String,
            request: CreateCommentRequest
    ): CreateCommentApiResponse {
        return executeApiCall(
                operation = "POST /project/v1/projects/$projectId/posts/$postId/logs",
                successMessage = "✅ 업무 댓글 생성 성공"
        ) {
            httpClient.post("/project/v1/projects/$projectId/posts/$postId/logs") {
                setBody(request)
            }
        }
    }

    override suspend fun getPostComments(
            projectId: String,
            postId: String,
            page: Int?,
            size: Int?,
            order: String?
    ): PostCommentListResponse {
        return executeApiCall(
                operation = "GET /project/v1/projects/$projectId/posts/$postId/logs",
                successMessage = "✅ 업무 댓글 목록 조회 성공"
        ) {
            httpClient.get("/project/v1/projects/$projectId/posts/$postId/logs") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
                order?.let { parameter("order", it) }
            }
        }
    }

    override suspend fun getPostComment(
            projectId: String,
            postId: String,
            logId: String
    ): PostCommentDetailResponse {
        return executeApiCall(
                operation = "GET /project/v1/projects/$projectId/posts/$postId/logs/$logId",
                successMessage = "✅ 업무 댓글 상세 조회 성공"
        ) { httpClient.get("/project/v1/projects/$projectId/posts/$postId/logs/$logId") }
    }

    override suspend fun updatePostComment(
            projectId: String,
            postId: String,
            logId: String,
            request: UpdateCommentRequest
    ): UpdateCommentResponse {
        return executeApiCallForNullableResult(
                operation = "PUT /project/v1/projects/$projectId/posts/$postId/logs/$logId",
                successMessage = "✅ 업무 댓글 수정 성공"
        ) {
            httpClient.put("/project/v1/projects/$projectId/posts/$postId/logs/$logId") {
                setBody(request)
            }
        }
    }

    override suspend fun deletePostComment(
            projectId: String,
            postId: String,
            logId: String
    ): DeleteCommentResponse {
        return executeApiCallForNullableResult(
                operation = "DELETE /project/v1/projects/$projectId/posts/$postId/logs/$logId",
                successMessage = "✅ 업무 댓글 삭제 성공"
        ) { httpClient.delete("/project/v1/projects/$projectId/posts/$postId/logs/$logId") }
    }

    // ============ 프로젝트 관련 API 구현 ============

    override suspend fun getProjects(
            page: Int?,
            size: Int?,
            type: String?,
            scope: String?,
            state: String?
    ): ProjectListResponse {
        return executeApiCall(
                operation = "GET /project/v1/projects",
                successMessage = "✅ 프로젝트 목록 조회 성공"
        ) {
            httpClient.get("/project/v1/projects") {
                parameter("member", "me")
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
                type?.let { parameter("type", it) }
                scope?.let { parameter("scope", it) }
                state?.let { parameter("state", it) }
            }
        }
    }

    // ============ 메신저 관련 API 구현 ============

    override suspend fun searchMembers(
            name: String?,
            externalEmailAddresses: List<String>?,
            userCode: String?,
            idProviderUserId: String?,
            page: Int?,
            size: Int?
    ): MemberSearchResponse {
        return executeApiCall(
                operation = "GET /common/v1/members",
                successMessage = "✅ 멤버 검색 성공"
        ) {
            httpClient.get("/common/v1/members") {
                name?.let { parameter("name", it) }
                externalEmailAddresses?.let { 
                    parameter("externalEmailAddresses", it.joinToString(",")) 
                }
                userCode?.let { parameter("userCode", it) }
                idProviderUserId?.let { parameter("idProviderUserId", it) }
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
            }
        }
    }

    override suspend fun sendDirectMessage(request: DirectMessageRequest): DirectMessageResponse {
        return executeApiCallForNullableResult(
                operation = "POST /messenger/v1/channels/direct-send",
                successMessage = "✅ 다이렉트 메시지 전송 성공"
        ) {
            httpClient.post("/messenger/v1/channels/direct-send") {
                setBody(request)
            }
        }
    }

    override suspend fun getChannels(
        page: Int?,
        size: Int?,
        recentMonths: Int?
    ): ChannelListResponse {
        val response = executeApiCall<ChannelListResponse>(
                operation = "GET /messenger/v1/channels",
                successMessage = "✅ 채널 목록 조회 성공"
        ) { 
            httpClient.get("/messenger/v1/channels") {
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
            }
        }
        
        // recentMonths가 지정된 경우 클라이언트 사이드에서 필터링
        return if (recentMonths != null && recentMonths > 0) {
            val cutoffDate = java.time.LocalDateTime.now().minusMonths(recentMonths.toLong())
            val filteredChannels = response.result.filter { channel ->
                try {
                    val updatedAt = java.time.LocalDateTime.parse(
                        channel.updatedAt?.replace(Regex("\\+09:00$"), "")?.split(".")?.get(0) ?: return@filter false
                    )
                    updatedAt.isAfter(cutoffDate)
                } catch (e: Exception) {
                    log.warn("날짜 파싱 실패 for channel ${channel.id}: ${channel.updatedAt}")
                    false
                }
            }
            log.info("🔍 최근 ${recentMonths}개월 필터링: ${response.result.size}개 → ${filteredChannels.size}개 채널")
            ChannelListResponse(
                header = response.header,
                result = filteredChannels,
                totalCount = filteredChannels.size
            )
        } else {
            response
        }
    }

    override suspend fun getSimpleChannels(
        page: Int?,
        size: Int?,
        recentMonths: Int?
    ): SimpleChannelListResponse {
        // 기존 getChannels를 재사용하여 데이터를 가져온 후, SimpleChannel로 변환
        val response = getChannels(page, size, recentMonths)
        
        val simpleChannels = response.result.map { channel ->
            SimpleChannel(
                id = channel.id,
                title = channel.title,
                type = channel.type,
                status = channel.status,
                updatedAt = channel.updatedAt,
                participantCount = channel.users?.participants?.size
            )
        }
        
        log.info("✂️ 채널 정보 간소화: 상세 정보 제거, ${response.result.size}개 채널 → 간단 정보만")
        
        return SimpleChannelListResponse(
            header = response.header,
            result = simpleChannels,
            totalCount = response.totalCount
        )
    }

    override suspend fun getChannel(channelId: String): Channel? {
        return try {
            // 전체 채널 목록에서 특정 채널을 찾아서 반환
            val response = getChannels()
            if (response.header.isSuccessful) {
                val channel = response.result.find { it.id == channelId }
                if (channel != null) {
                    log.info("✅ 채널 정보 조회 성공: ${channel.title} (ID: $channelId)")
                } else {
                    log.warn("⚠️ 채널을 찾을 수 없습니다: ID=$channelId")
                }
                channel
            } else {
                log.error("❌ 채널 목록 조회 실패: ${response.header.resultMessage}")
                null
            }
        } catch (e: Exception) {
            log.error("❌ 채널 조회 중 오류 발생: ${e.message}")
            null
        }
    }

    override suspend fun createChannel(request: CreateChannelRequest, idType: String?): CreateChannelResponse {
        return executeApiCall(
                operation = "POST /messenger/v1/channels",
                successMessage = "✅ 채널 생성 성공",
                expectedStatusCode = HttpStatusCode.OK // API 스펙에 따르면 200 응답
        ) {
            httpClient.post("/messenger/v1/channels") {
                setBody(request)
                // idType 파라미터가 제공된 경우에만 추가 (email 또는 memberId)
                idType?.let { parameter("idType", it) }
            }
        }
    }

    override suspend fun joinChannel(
            channelId: String,
            request: JoinChannelRequest
    ): JoinChannelResponse {
        return executeApiCallForNullableResult(
                operation = "POST /messenger/v1/channels/$channelId/members/join",
                successMessage = "✅ 채널 가입 성공"
        ) {
            httpClient.post("/messenger/v1/channels/$channelId/members/join") {
                setBody(request)
            }
        }
    }

    override suspend fun leaveChannel(channelId: String, request: LeaveChannelRequest): LeaveChannelResponse {
        return executeApiCallForNullableResult(
                operation = "POST /messenger/v1/channels/$channelId/members/leave",
                successMessage = "✅ 채널에서 멤버 제거 성공"
        ) { 
            httpClient.post("/messenger/v1/channels/$channelId/members/leave") {
                setBody(request)
            }
        }
    }

    // ⚠️ 채널 로그 조회는 Dooray API에서 지원하지 않음 (보안상 제한)
    // override suspend fun getChannelLogs(...): ChannelLogsResponse {...}

    override suspend fun sendChannelMessage(
            channelId: String,
            request: SendChannelMessageRequest
    ): SendChannelMessageResponse {
        return executeApiCallForNullableResult(
                operation = "POST /messenger/v1/channels/$channelId/logs",
                expectedStatusCode = HttpStatusCode.OK, // API 스펙에 따르면 200 응답
                successMessage = "✅ 채널 메시지 전송 성공"
        ) {
            httpClient.post("/messenger/v1/channels/$channelId/logs") {
                setBody(request)
            }
        }
    }

    // ============ 캘린더 관련 API ============

    override suspend fun getCalendars(): CalendarListResponse {
        return executeApiCall(
                operation = "GET /calendar/v1/calendars",
                successMessage = "✅ 캘린더 목록 조회 성공"
        ) {
            httpClient.get("/calendar/v1/calendars")
        }
    }

    override suspend fun getCalendarDetail(calendarId: String): CalendarDetailResponse {
        return executeApiCall(
                operation = "GET /calendar/v1/calendars/$calendarId",
                successMessage = "✅ 캘린더 상세 조회 성공"
        ) {
            httpClient.get("/calendar/v1/calendars/$calendarId")
        }
    }

    override suspend fun getCalendarEvents(
            calendars: String?,
            timeMin: String,
            timeMax: String,
            postType: String?,
            category: String?
    ): CalendarEventsResponse {
        return executeApiCall(
                operation = "GET /calendar/v1/calendars/*/events",
                successMessage = "✅ 캘린더 일정 조회 성공"
        ) {
            httpClient.get("/calendar/v1/calendars/*/events") {
                parameter("timeMin", timeMin)
                parameter("timeMax", timeMax)
                calendars?.let { parameter("calendars", it) }
                postType?.let { parameter("postType", it) }
                category?.let { parameter("category", it) }
            }
        }
    }

    override suspend fun getCalendarEventDetail(
            calendarId: String,
            eventId: String
    ): CalendarEventDetailResponse {
        return executeApiCall(
                operation = "GET /calendar/v1/calendars/$calendarId/events/$eventId",
                successMessage = "✅ 캘린더 일정 상세 조회 성공"
        ) {
            httpClient.get("/calendar/v1/calendars/$calendarId/events/$eventId")
        }
    }

    override suspend fun createCalendarEvent(
            calendarId: String,
            request: CreateCalendarEventRequest
    ): CalendarEventCreateResponse {
        return executeApiCall(
                operation = "POST /calendar/v1/calendars/$calendarId/events",
                expectedStatusCode = HttpStatusCode.OK,
                successMessage = "✅ 캘린더 일정 등록 성공"
        ) {
            httpClient.post("/calendar/v1/calendars/$calendarId/events") {
                setBody(request)
            }
        }
    }
}
