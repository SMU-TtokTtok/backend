# AGENTS 개발 가이드라인

이 문서는 AI 에이전트가 개발 작업 시 반드시 준수해야 할 코딩 원칙과 가이드라인을 정의합니다.

## 📋 목차
- [SOLID 원칙](#solid-원칙)
- [클린 코드 원칙](#클린-코드-원칙)
- [도메인 주도 설계 (DDD)](#도메인-주도-설계-ddd)
- [Spring Boot 특화 가이드라인](#spring-boot-특화-가이드라인)
- [코드 작성 규칙](#코드-작성-규칙)

---

## 🎯 SOLID 원칙

### S - Single Responsibility Principle (단일 책임 원칙)
- **하나의 클래스는 하나의 책임만 가져야 한다**
- 각 클래스와 메서드는 명확한 단일 목적을 가져야 함
- Controller는 HTTP 요청/응답 처리만, Service는 비즈니스 로직만, Repository는 데이터 접근만

```java
// ❌ Bad - 여러 책임을 가진 클래스
public class UserController {
    public void saveUser() { /* 저장 로직 */ }
    public void sendEmail() { /* 이메일 로직 */ }
    public void validateUser() { /* 검증 로직 */ }
}

// ✅ Good - 책임 분리
public class UserController { /* HTTP 처리만 */ }
public class UserService { /* 비즈니스 로직만 */ }
public class EmailService { /* 이메일 전송만 */ }
```

### O - Open/Closed Principle (개방/폐쇄 원칙)
- **확장에는 열려있고, 수정에는 닫혀있어야 한다**
- 인터페이스와 추상클래스를 활용하여 기능 확장
- 기존 코드 수정 없이 새로운 기능 추가

### L - Liskov Substitution Principle (리스코프 치환 원칙)
- **하위 타입은 상위 타입을 대체할 수 있어야 한다**
- 인터페이스 구현체들은 동일한 계약을 준수해야 함

### I - Interface Segregation Principle (인터페이스 분리 원칙)
- **클라이언트는 사용하지 않는 인터페이스에 의존하면 안 된다**
- 작고 구체적인 인터페이스를 선호

### D - Dependency Inversion Principle (의존성 역전 원칙)
- **고수준 모듈은 저수준 모듈에 의존해서는 안 된다**
- Spring DI 컨테이너를 활용한 의존성 주입 사용

---

## 🧹 클린 코드 원칙

### 1. 의미 있는 이름 사용
```java
// ❌ Bad
public List<Club> getData() { return clubs; }

// ✅ Good
public List<Club> getActiveClubs() { return activeClubs; }
```

### 2. 함수는 작고 단일 목적을 가져야 함
- 함수는 한 가지 일만 해야 함
- 함수 길이는 20줄 이내 권장
- 함수 인자는 3개 이하 권장

### 3. 주석보다는 코드로 설명
```java
// ❌ Bad
// 사용자가 활성화된 상태인지 확인
if (user.getStatus() == 1) { }

// ✅ Good
if (user.isActive()) { }
```

### 4. 일관된 포맷팅
- 들여쓰기 4칸 사용
- 중괄호는 K&R 스타일
- 한 줄 최대 120자

### 5. 예외 처리
- 체크드 예외보다는 언체크드 예외 사용
- 구체적인 예외 타입 정의
- 예외 처리는 최상위 레벨에서

---

## 🏗️ 도메인 주도 설계 (DDD)

### 1. 계층 구조
```
Controller (Presentation Layer)
    ↓
Service (Application Layer)  
    ↓
Domain (Domain Layer)
    ↓
Repository (Infrastructure Layer)
```

### 2. 도메인 모델 중심 설계
- **Entity**: 고유 식별자를 가진 도메인 객체
- **Value Object**: 값으로만 구별되는 불변 객체
- **Aggregate**: 데이터 변경의 단위
- **Repository**: 도메인 객체의 저장소 추상화

### 3. 패키지 구조
```
src/main/java/org/project/ttokttok/
├── domain/
│   ├── club/
│   │   ├── controller/     # 프레젠테이션 계층
│   │   ├── service/        # 애플리케이션 계층
│   │   ├── domain/         # 도메인 계층
│   │   └── repository/     # 인프라스트럭처 계층
│   └── user/
└── global/                 # 공통 기능
```

### 4. 도메인 규칙
- 비즈니스 로직은 도메인 객체 내부에 위치
- Service는 도메인 객체들을 조합하여 유스케이스 구현
- Controller는 요청/응답 변환만 담당

---

## 🌱 Spring Boot 특화 가이드라인

### 1. 어노테이션 사용
```java
@RestController
@RequiredArgsConstructor  // 생성자 주입
@Slf4j               // 로깅
@Tag(name = "API명") // Swagger 문서화
public class ClubController {
    private final ClubService clubService; // final 키워드 사용
}
```

### 2. 의존성 주입
- 생성자 주입 사용 (Lombok @RequiredArgsConstructor)
- 필드 주입과 Setter 주입 지양

### 3. 예외 처리
- @ControllerAdvice를 통한 전역 예외 처리
- 커스텀 예외 클래스 정의
- 적절한 HTTP 상태 코드 반환

### 4. 응답 형식 통일
```java
@GetMapping
public ResponseEntity<ApiResponse<ClubListResponse>> getClubs() {
    // 일관된 응답 형식 사용
    return ResponseEntity.ok(ApiResponse.success(data));
}
```

---

## 📝 코드 작성 규칙

### 1. 네이밍 컨벤션
- **클래스**: PascalCase (e.g., ClubService)
- **메서드/변수**: camelCase (e.g., getActiveClubs)
- **상수**: UPPER_SNAKE_CASE (e.g., MAX_MEMBER_COUNT)
- **패키지**: 소문자 (e.g., domain.club.service)

### 2. 메서드 작성 규칙
```java
// ✅ Good - 명확한 메서드명과 단일 책임
public ClubDetailResponse getClubIntroduction(String userEmail, String clubId) {
    validateUser(userEmail);
    Club club = findClubById(clubId);
    return ClubDetailResponse.from(club);
}
```

### 3. DTO 변환 규칙
- Entity ↔ DTO 변환은 정적 팩토리 메서드 사용
- `from()`, `to()` 메서드명 사용

### 4. 테스트 작성
- 단위 테스트 필수 작성
- Given-When-Then 패턴 사용
- 테스트 메서드명은 한글 허용

### 5. 로깅
```java
@Slf4j
public class ClubService {
    public void processClub(String clubId) {
        log.info("동아리 처리 시작: clubId={}", clubId);
        // 비즈니스 로직
        log.info("동아리 처리 완료: clubId={}", clubId);
    }
}
```

### 6. 작업 기록 관리
- 특정한 날짜마다 작업한 기록들을 `implementation.md`로 작성하고 작업마다 갱신해야 함

---

## ⚠️ 금지 사항

1. **God Object 생성 금지** - 하나의 클래스에 너무 많은 책임 부여 금지
2. **매직 넘버 사용 금지** - 상수로 정의하여 사용
3. **Primitive Obsession 금지** - 원시 타입 남용 금지, Value Object 활용
4. **강한 결합 금지** - 인터페이스를 통한 느슨한 결합 유지
5. **비즈니스 로직을 Controller에 작성 금지**

---

## 🔍 코드 리뷰 체크리스트

- [ ] SOLID 원칙 준수 여부
- [ ] 클린 코드 원칙 적용 여부  
- [ ] DDD 계층 구조 준수 여부
- [ ] 네이밍 컨벤션 준수 여부
- [ ] 예외 처리 적절성
- [ ] 테스트 코드 존재 여부
- [ ] 문서화 (Swagger) 완성도

---

**이 가이드라인을 준수하여 유지보수 가능하고 확장 가능한 고품질 코드를 작성하시기 바랍니다.**
