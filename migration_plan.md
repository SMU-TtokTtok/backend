# Global 패키지 Validation 정리 방안

## 현재 상황
- feature/#4에서 `global/validation/` 패키지 추가
- 기존 `global/annotation/`, `global/annotationresolver/`와 중복 구조 발생

## 제안: Option 1 - 기능별 통합 구조

### 1단계: 디렉토리 구조 변경
```bash
# 새로운 구조 생성
mkdir -p src/main/java/org/project/ttokttok/global/annotation/auth
mkdir -p src/main/java/org/project/ttokttok/global/annotation/validation
mkdir -p src/main/java/org/project/ttokttok/global/resolver/auth
mkdir -p src/main/java/org/project/ttokttok/global/resolver/validation

# 파일 이동
mv global/annotation/AuthUserInfo.java global/annotation/auth/
mv global/validation/annotation/StrongPassword.java global/annotation/validation/
mv global/annotationresolver/AuthUserInfoResolver.java global/resolver/auth/
mv global/validation/validator/StrongPasswordValidator.java global/resolver/validation/
```

### 2단계: 패키지명 수정
- 모든 파일의 package 선언 수정
- import 문 수정

### 3단계: 기존 디렉토리 정리
```bash
rm -rf global/annotationresolver
rm -rf global/validation
```

### 4단계: 설정 파일 업데이트
- WebMvcConfig.java의 import 경로 수정
- 기타 참조 파일들 수정

## 예상 변경 파일 목록
1. AuthUserInfo.java - 패키지명 변경
2. StrongPassword.java - 패키지명 변경  
3. AuthUserInfoResolver.java - 패키지명 변경
4. StrongPasswordValidator.java - 패키지명 변경
5. WebMvcConfig.java - import 수정
6. 기타 참조 파일들

## 검증 방법
1. 컴파일 에러 없는지 확인
2. 테스트 실행
3. 기능 동작 확인 