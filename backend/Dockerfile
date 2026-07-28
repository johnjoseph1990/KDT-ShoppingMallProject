# ── 빌드 스테이지 ────────────────────────────────────────────────────────────
# eclipse-temurin: OpenJDK의 공식 Docker 배포판. jdk는 컴파일(javac)까지 포함.
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# gradle wrapper와 의존성 캐시 레이어를 소스 복사 전에 분리해두면
# 소스만 바뀌었을 때 의존성 다운로드 레이어를 재사용해 빌드가 빨라진다.
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src/ src/
# -x test: 테스트는 CI에서 이미 검증했으므로 Docker 빌드 시 건너뜀
RUN ./gradlew bootJar -x test --no-daemon

# ── 실행 스테이지 ─────────────────────────────────────────────────────────────
# jre: 런타임만 포함. jdk 대비 이미지 크기가 절반 이하.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
