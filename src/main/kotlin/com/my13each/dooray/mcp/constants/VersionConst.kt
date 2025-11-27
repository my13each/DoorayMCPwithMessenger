package com.my13each.dooray.mcp.constants

/** 프로젝트 버전 상수 */
object VersionConst {
    /** 현재 프로젝트 버전 (gradle.properties의 project.version과 동일하게 유지) */
    const val VERSION = "0.2.28"

    /** 버전 히스토리 및 변경사항 */
    const val CHANGELOG =
            """
        0.2.28 - 업무 목록 담당자 정보 추가 경량화 (이름/ID만 포함)
        0.2.27 - 업무 목록 조회 최적화 (경량화 응답, 최대 250건 지원)
        0.2.26 - 환경변수 기반 툴 카테고리 필터링 추가 (DOORAY_ENABLED_CATEGORIES)
        0.2.25 - 드라이브 변경 이력 조회 기능 추가 (getDriveChanges)
        0.2.24 - 파일명 변경 API 지원 추가 (renameFile)
        0.2.23 - 드라이브 파일 목록 조회 타입/서브타입 필터링 추가
        0.2.22 - 드라이브 상세 조회 및 멤버 관리 기능 추가
        0.2.21 - 드라이브 목록 조회 필터링 기능 추가 (프로젝트/타입/스코프/상태)
        0.2.1 - HTTP 로깅 stdout 오염 문제 수정, 환경변수를 통한 로깅 레벨 제어 추가
        0.2.0 - kotlin-mcp-sdk 0.6.0 업데이트, 라이브러리 메이저 버전 업그레이드
        0.1.7 - MCP 도구 단위 테스트 추가 (MockK 기반), CI/CD 파이프라인 분리 (PR/Main), GitHub Actions 워크플로우 개선
        0.1.6 - 업무 수정 및 댓글 관리 도구 추가 (총 19개 도구)
        0.1.5 - result: null 응답 처리 개선, 업무 상태 변경 API 안정화
        0.1.4 - 위키 및 업무 CRUD 도구 완성 (13개 도구)
        0.1.3 - 위키 페이지 생성/수정 도구 추가
        0.1.2 - 기본 위키 조회 도구 구현
        0.1.1 - 초기 MCP 서버 구조 설정
        0.1.0 - 프로젝트 초기화
    """
}
