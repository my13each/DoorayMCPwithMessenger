package com.bifos.dooray.mcp.client

import com.bifos.dooray.mcp.types.*

interface DoorayClient {

    /** 접근 가능한 위키 목록을 조회합니다. */
    suspend fun getWikis(page: Int? = null, size: Int? = null): WikiListResponse

    /** 특정 프로젝트의 위키 목록을 조회합니다. */
    suspend fun getWikiPages(projectId: String): WikiPagesResponse

    /** 특정 상위 페이지의 자식 위키 페이지들을 조회합니다. */
    suspend fun getWikiPages(projectId: String, parentPageId: String): WikiPagesResponse

    /** 특정 위키 페이지의 상세 정보를 조회합니다. */
    suspend fun getWikiPage(projectId: String, pageId: String): WikiPageResponse

    /** 새로운 위키 페이지를 생성합니다. */
    suspend fun createWikiPage(
        wikiId: String,
        request: CreateWikiPageRequest
    ): CreateWikiPageResponse

    /** 위키 페이지를 수정합니다. */
    suspend fun updateWikiPage(
        wikiId: String,
        pageId: String,
        request: UpdateWikiPageRequest
    ): DoorayApiUnitResponse

    // ============ 프로젝트 업무 관련 API ============

    /** 프로젝트 내에 업무를 생성합니다. */
    suspend fun createPost(projectId: String, request: CreatePostRequest): CreatePostApiResponse

    /** 업무 목록을 조회합니다. */
    suspend fun getPosts(
        projectId: String,
        page: Int? = null,
        size: Int? = null,
        fromMemberIds: List<String>? = null,
        toMemberIds: List<String>? = null,
        ccMemberIds: List<String>? = null,
        tagIds: List<String>? = null,
        parentPostId: String? = null,
        postNumber: String? = null,
        postWorkflowClasses: List<String>? = null,
        postWorkflowIds: List<String>? = null,
        milestoneIds: List<String>? = null,
        subjects: String? = null,
        createdAt: String? = null,
        updatedAt: String? = null,
        dueAt: String? = null,
        order: String? = null
    ): PostListResponse

    /** 업무 상세 정보를 조회합니다. */
    suspend fun getPost(projectId: String, postId: String): PostDetailResponse

    /** 업무를 수정합니다. */
    suspend fun updatePost(
        projectId: String,
        postId: String,
        request: UpdatePostRequest
    ): UpdatePostResponse

    /** 특정 담당자의 상태를 변경합니다. */
    suspend fun updatePostUserWorkflow(
        projectId: String,
        postId: String,
        organizationMemberId: String,
        workflowId: String
    ): DoorayApiUnitResponse

    /** 업무 전체의 상태를 변경합니다. */
    suspend fun setPostWorkflow(
        projectId: String,
        postId: String,
        workflowId: String
    ): DoorayApiUnitResponse

    /** 업무 상태를 완료로 변경합니다. */
    suspend fun setPostDone(projectId: String, postId: String): DoorayApiUnitResponse

    /** 업무의 상위 업무를 설정합니다. */
    suspend fun setPostParent(
        projectId: String,
        postId: String,
        parentPostId: String
    ): DoorayApiUnitResponse

    // ============ 업무 댓글 관련 API ============

    /** 업무에 댓글을 생성합니다. */
    suspend fun createPostComment(
        projectId: String,
        postId: String,
        request: CreateCommentRequest
    ): CreateCommentApiResponse

    /** 업무 댓글 목록을 조회합니다. */
    suspend fun getPostComments(
        projectId: String,
        postId: String,
        page: Int? = null,
        size: Int? = null,
        order: String? = null
    ): PostCommentListResponse

    /** 특정 업무 댓글의 상세 정보를 조회합니다. */
    suspend fun getPostComment(
        projectId: String,
        postId: String,
        logId: String
    ): PostCommentDetailResponse

    /** 업무 댓글을 수정합니다. */
    suspend fun updatePostComment(
        projectId: String,
        postId: String,
        logId: String,
        request: UpdateCommentRequest
    ): UpdateCommentResponse

    /** 업무 댓글을 삭제합니다. */
    suspend fun deletePostComment(
        projectId: String,
        postId: String,
        logId: String
    ): DeleteCommentResponse

    // ============ 프로젝트 관련 API ============
    suspend fun getProjects(
        page: Int? = null,
        size: Int? = null,
        type: String? = null,
        scope: String? = null,
        state: String? = null
    ): ProjectListResponse

    // ============ 메신저 관련 API ============

    /** 멤버를 검색합니다. */
    suspend fun searchMembers(
        name: String? = null,
        externalEmailAddresses: List<String>? = null,
        userCode: String? = null,
        idProviderUserId: String? = null,
        page: Int? = null,
        size: Int? = null
    ): MemberSearchResponse

    /** 1:1 다이렉트 메시지를 전송합니다. */
    suspend fun sendDirectMessage(request: DirectMessageRequest): DirectMessageResponse

    /** 접근 가능한 채널 목록을 조회합니다. */
    suspend fun getChannels(
        page: Int? = null,
        size: Int? = null,
        recentMonths: Int? = null
    ): ChannelListResponse

    /** 간단한 채널 목록을 조회합니다. (검색용, 대용량 데이터 방지) */
    suspend fun getSimpleChannels(
        page: Int? = null,
        size: Int? = null,
        recentMonths: Int? = null
    ): SimpleChannelListResponse

    /** 특정 채널의 상세 정보를 조회합니다. */
    suspend fun getChannel(channelId: String): Channel?

    /** 새 채널을 생성합니다. */
    suspend fun createChannel(request: CreateChannelRequest, idType: String? = null): CreateChannelResponse

    /** 채널에 멤버를 가입시킵니다. */
    suspend fun joinChannel(channelId: String, request: JoinChannelRequest): JoinChannelResponse

    /** 채널에서 멤버를 제거합니다. */
    suspend fun leaveChannel(channelId: String, request: LeaveChannelRequest): LeaveChannelResponse

    // ⚠️ 채널 로그 조회는 Dooray API에서 지원하지 않음 (보안상 제한)
    // suspend fun getChannelLogs(...): ChannelLogsResponse

    /** 채널에 메시지를 전송합니다. */
    suspend fun sendChannelMessage(
        channelId: String,
        request: SendChannelMessageRequest
    ): SendChannelMessageResponse

    // ============ 캘린더 관련 API ============

    /** 접근 가능한 캘린더 목록을 조회합니다. */
    suspend fun getCalendars(): CalendarListResponse

    /** 특정 캘린더의 상세 정보를 조회합니다. */
    suspend fun getCalendarDetail(calendarId: String): CalendarDetailResponse

    /** 특정 기간의 캘린더 일정을 조회합니다. */
    suspend fun getCalendarEvents(
        calendars: String? = null,
        timeMin: String,
        timeMax: String,
        postType: String? = null,
        category: String? = null
    ): CalendarEventsResponse

    /** 특정 캘린더 일정의 상세 정보를 조회합니다. */
    suspend fun getCalendarEventDetail(
        calendarId: String,
        eventId: String
    ): CalendarEventDetailResponse

    /** 캘린더에 새로운 일정을 등록합니다. */
    suspend fun createCalendarEvent(
        calendarId: String,
        request: CreateCalendarEventRequest
    ): CalendarEventCreateResponse
}
