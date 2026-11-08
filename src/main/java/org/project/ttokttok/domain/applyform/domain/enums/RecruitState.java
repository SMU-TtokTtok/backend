package org.project.ttokttok.domain.applyform.domain.enums;

/**
 * 지원 폼의 모집 상태. {@code RecruitStatePolicy} 가 판정한다.
 */
public enum RecruitState {
    UPCOMING, // 활성, 모집 스위치 켜짐, 시작일 전
    OPEN,     // 활성, 모집 스위치 켜짐, 시작일 ~ 마감일
    CLOSED,   // 활성인데 모집 스위치가 꺼졌거나 마감일이 지남
    FINISHED  // 비활성. 지원자 초기화(평가 종료)를 마친 폼
}
