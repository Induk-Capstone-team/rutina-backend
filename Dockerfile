# =========================================
# Build - 소스코드를 컴파일하여 JAR 파일 생성
# JDK(Java Development Kit) = 컴파일러 포함, 빌드할 때만 필요
# =========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle wrapper와 의존성 설정 파일만 먼저 복사
# 소스코드보다 먼저 복사하는 이유:
# Docker는 변경된 레이어부터 다시 빌드하는데,
# 소스코드는 자주 바뀌지만 의존성 파일은 잘 안 바뀜
# → 의존성 다운로드 레이어를 캐시해두면 빌드 속도가 훨씬 빨라짐
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 미리 다운로드 (캐시 목적)
# --no-daemon: CI 환경에서 Gradle 데몬 프로세스 띄우지 않음 (메모리 절약)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스코드 복사 (의존성 캐시 이후에 복사해야 캐시 효과 있음)
COPY src src

# bootJar: Spring Boot 실행 가능한 JAR 파일 생성
# 결과물: build/libs/*.jar
RUN ./gradlew bootJar --no-daemon

# =========================================
# Run - 빌드된 JAR 파일만 가져와서 실행
# JRE(Java Runtime Environment) = 실행만 가능, JDK보다 훨씬 가벼움
# JDK 이미지 ~300MB → JRE 이미지 ~85MB
# =========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Build 단계에서 생성된 JAR 파일만 복사 (소스코드, Gradle 등은 포함 안 됨)
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너가 사용할 포트 명시 (실제 포트 오픈은 docker-compose에서 처리)
EXPOSE 8080

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]