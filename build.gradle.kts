plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "SimpleDb-2509"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Lombok: getter/setter 등 반복 코드를 어노테이션으로 자동 생성
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // JUnit 5: 테스트 프레임워크
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MySQL JDBC 드라이버: Java가 MySQL과 대화하기 위해 반드시 필요
    implementation("com.mysql:mysql-connector-j:9.3.0")

    // AssertJ: 테스트 결과 검증을 편하게 해주는 라이브러리
    testImplementation("org.assertj:assertj-core:3.27.3")

    // Jackson: Map ↔ 자바 객체 변환 (DB 결과를 Article로 바꿀 때 사용)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    // Jackson의 LocalDateTime 지원 확장
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
