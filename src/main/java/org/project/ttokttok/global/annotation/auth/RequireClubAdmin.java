package org.project.ttokttok.global.annotation.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 권한 및 동아리 소유권을 검증하는 어노테이션입니다.
 * AOP를 통해 메서드 실행 전 관리자 여부를 확인하고, 
 * 해당 관리자가 운영하는 동아리(Club) 정보를 ClubHolder에 저장합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireClubAdmin {
}
