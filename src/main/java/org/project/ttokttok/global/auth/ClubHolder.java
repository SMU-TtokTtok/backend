package org.project.ttokttok.global.auth;

import org.project.ttokttok.domain.club.domain.Club;

/**
 * 현재 요청을 처리하는 스레드 내에서 인증된 관리자의 동아리 정보를 관리하는 홀더 클래스입니다.
 * AOP에서 검증된 Club 객체를 저장하고, 서비스 레이어에서 이를 조회할 수 있도록 지원합니다.
 */
public class ClubHolder {

    private static final ThreadLocal<Club> clubThreadLocal = new ThreadLocal<>();

    public static void setClub(Club club) {
        clubThreadLocal.set(club);
    }

    public static Club getClub() {
        return clubThreadLocal.get();
    }

    public static void clear() {
        clubThreadLocal.remove();
    }
}
