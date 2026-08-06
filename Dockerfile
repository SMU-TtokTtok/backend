# 빌드 스테이지 — 의존성 해석을 소스 복사보다 먼저 두어, 소스만 바뀐 재빌드에서
# 의존성 레이어가 캐시에 남도록 한다.
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S ttokttok && adduser -S -G ttokttok ttokttok

# bootJar 는 실행 가능한 부트 jar 하나만 만든다(`build` 와 달리 *-plain.jar 를 만들지 않는다).
COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar

# 운영 설정(application-prod.yml)과 Firebase 서비스 계정 키는 이미지에 굽지 않는다.
# 런타임에 이 디렉터리로 마운트하며, Spring Boot 가 ./config/ 를 기본 설정 탐색 경로에
# 포함하므로 application-prod.yml 은 별도 옵션 없이 읽힌다.
# Firebase 키는 classpath 가 아니므로 firebase.credentials-location 에 file: 경로를 준다.
RUN mkdir -p /app/config && chown -R ttokttok:ttokttok /app

USER ttokttok
EXPOSE 8080

# 힙을 고정값(-Xmx)이 아니라 비율로 잡아, compose 의 메모리 제한만 조정해도 힙이 따라오게 한다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# exec 로 넘겨 java 가 PID 1 이 되게 한다. SIGTERM 이 셸에 먹히지 않고 그대로 전달되어야
# Spring 의 graceful shutdown 이 동작한다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
