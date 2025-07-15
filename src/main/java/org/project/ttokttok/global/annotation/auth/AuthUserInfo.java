package org.project.ttokttok.global.annotation.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUserInfo {
    // 컨트롤러 파라미터에 String username 넣고 앞에 달아주면,
    // jwt 액세스 토큰에서 사용자명 자동 추출
}
