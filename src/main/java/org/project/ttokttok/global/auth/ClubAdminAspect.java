package org.project.ttokttok.global.auth;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Aspect
@Component
@RequiredArgsConstructor
public class ClubAdminAspect {

    private final ClubRepository clubRepository;

    @Around("@annotation(org.project.ttokttok.global.annotation.auth.RequireClubAdmin)")
    public Object validateClubAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        String username = findAdminUsername(joinPoint);
        String requestedClubId = findClubIdArgument(joinPoint);

        Club club = clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);

        // 만약 파라미터로 clubId가 넘어온 경우, 관리자의 동아리 ID와 일치하는지 추가 검증
        if (requestedClubId != null && !club.getId().equals(requestedClubId)) {
            throw new NotClubAdminException();
        }

        try {
            ClubHolder.setClub(club);
            return joinPoint.proceed();
        } finally {
            ClubHolder.clear();
        }
    }

    private String findAdminUsername(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof AuthUserInfo) {
                    return (String) args[i];
                }
            }
        }
        
        // AuthUserInfo 어노테이션이 없는 경우 첫 번째 인자를 username으로 가정 (기존 관례)
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }

        throw new IllegalArgumentException("관리자 정보를 찾을 수 없습니다.");
    }

    private String findClubIdArgument(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames == null) return null;

        for (int i = 0; i < parameterNames.length; i++) {
            if ("clubId".equals(parameterNames[i]) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }
}
