package com.my13each.dooray.mcp.constants

/**
 * Dooray MCP 도구 카테고리
 *
 * 환경변수 DOORAY_ENABLED_CATEGORIES로 활성화할 카테고리를 제어할 수 있습니다.
 * 예: DOORAY_ENABLED_CATEGORIES="wiki,project"
 *
 * 비어있거나 미지정시 모든 카테고리가 활성화됩니다.
 */
enum class ToolCategory {
    /**
     * Wiki 관련 도구 (5개)
     * - 위키 프로젝트 목록, 페이지 목록/상세, 페이지 생성/수정
     */
    WIKI,

    /**
     * Project 및 업무 관련 도구 (11개)
     * - 프로젝트 목록, 업무 CRUD, 업무 상태 변경
     * - 업무 댓글 CRUD 포함
     */
    PROJECT,

    /**
     * Messenger 관련 도구 (7개)
     * - 멤버 검색, DM 전송, 채널 관리, 메시지 전송
     */
    MESSENGER,

    /**
     * Calendar 관련 도구 (5개)
     * - 캘린더 목록/상세, 일정 조회/상세/생성
     */
    CALENDAR,

    /**
     * Drive 및 공유링크 관련 도구 (19개)
     * - 드라이브/파일 관리, 업로드/다운로드
     * - 공유링크 CRUD 포함
     */
    DRIVE;

    companion object {
        /**
         * 환경변수에서 활성화된 카테고리 목록을 파싱합니다.
         *
         * @param envValue 환경변수 값 (예: "wiki,project,messenger")
         * @return 활성화된 카테고리 Set. null이나 빈 값이면 모든 카테고리 반환.
         */
        fun parseEnabledCategories(envValue: String?): Set<ToolCategory> {
            if (envValue.isNullOrBlank()) {
                // 환경변수가 없으면 모든 카테고리 활성화
                return values().toSet()
            }

            return envValue.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .mapNotNull { categoryName ->
                    try {
                        valueOf(categoryName)
                    } catch (e: IllegalArgumentException) {
                        // 잘못된 카테고리 이름은 무시
                        System.err.println("Warning: Unknown category '$categoryName' in DOORAY_ENABLED_CATEGORIES")
                        null
                    }
                }
                .toSet()
                .ifEmpty {
                    // 파싱 결과가 비어있으면 모든 카테고리 활성화
                    System.err.println("Warning: No valid categories found in DOORAY_ENABLED_CATEGORIES. Enabling all categories.")
                    values().toSet()
                }
        }
    }
}
