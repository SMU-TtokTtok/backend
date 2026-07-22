package org.project.ttokttok.global.entity;

public enum Role { // 사용자 역할 구분용 -> 액세스 토큰에 사용
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN // 서비스 운영/유지보수 팀 (전역 공지사항 작성 권한)
}
