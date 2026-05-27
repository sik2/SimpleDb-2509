# SimpleDb 구현 교안

> MySQL/JDBC 기반 데이터베이스 유틸리티 클래스 직접 구현하기

---

## 목차

1. [이 교안에서 만드는 것](#1-이-교안에서-만드는-것)
2. [사전 지식 — JDBC란?](#2-사전-지식--jdbc란)
3. [프로젝트 세팅](#3-프로젝트-세팅)
4. [Article 클래스 구현](#4-article-클래스-구현)
5. [SimpleDb 클래스 구현](#5-simpledb-클래스-구현)
6. [Sql 클래스 구현](#6-sql-클래스-구현)
7. [테스트 실행](#7-테스트-실행)
8. [자주 하는 실수](#8-자주-하는-실수)

---

## 1. 이 교안에서 만드는 것

### 목표

Java 코드에서 MySQL 데이터베이스를 **편리하게** 사용할 수 있는 유틸리티 클래스를 직접 만듭니다.

### 완성 후 사용 예시

```java
// SimpleDb 객체 하나만 만들면 됩니다
SimpleDb simpleDb = new SimpleDb("localhost", "root", "비밀번호", "데이터베이스명");

// SQL을 이렇게 간편하게 작성합니다
Sql sql = simpleDb.genSql();
sql.append("SELECT * FROM article WHERE id = ?", 1);

Article article = sql.selectRow(Article.class); // 결과를 자바 객체로 바로 받기
```

### 만들 파일 3가지

| 파일 | 역할 |
|------|------|
| `Article.java` | 게시글 데이터를 담는 그릇 (VO) |
| `SimpleDb.java` | DB 연결을 관리하는 핵심 클래스 |
| `Sql.java` | SQL을 조립하고 실행하는 클래스 |

---

## 2. 사전 지식 — JDBC란?

### JDBC = Java DataBase Connectivity

Java 프로그램이 데이터베이스와 대화하기 위한 표준 인터페이스입니다.

```
Java 코드 → JDBC API → MySQL JDBC 드라이버 → MySQL 서버
```

### JDBC의 기본 흐름 (꼭 이해하세요!)

```java
// 1단계: DB에 연결 (전화 연결하기)
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/mydb", "root", "1234");

// 2단계: SQL 준비 (메모 작성하기)
PreparedStatement ps = conn.prepareStatement("SELECT * FROM article WHERE id = ?");

// 3단계: ? 자리에 값 채우기
ps.setObject(1, 1L);  // 첫 번째 ?에 1 넣기

// 4단계: SQL 실행 후 결과 받기
ResultSet rs = ps.executeQuery();

// 5단계: 결과 읽기
while (rs.next()) {
    Long id = rs.getLong("id");
    String title = rs.getString("title");
}

// 6단계: 자원 닫기 (전화 끊기)
rs.close();
ps.close();
conn.close();
```

### PreparedStatement vs Statement

| 구분 | Statement | PreparedStatement |
|------|-----------|-------------------|
| 사용법 | SQL을 문자열로 직접 작성 | ?로 자리표시 후 값을 따로 전달 |
| 보안 | SQL Injection 위험 | 안전 |
| 예시 | `"WHERE id = " + id` | `"WHERE id = ?"` |

> **항상 PreparedStatement를 사용하세요!**

### ThreadLocal이란?

멀티쓰레드 환경에서 **각 쓰레드가 자기만의 변수**를 가질 수 있게 해주는 클래스입니다.

```
쓰레드 A ──┐
            ├── simpleDb (하나의 객체 공유)
쓰레드 B ──┘

쓰레드 A의 Connection: conn-A  (ThreadLocal에 저장, A만 접근 가능)
쓰레드 B의 Connection: conn-B  (ThreadLocal에 저장, B만 접근 가능)
```

일반 변수를 쓰면:
- 쓰레드 A가 Connection을 사용하는 도중 쓰레드 B가 끊어버릴 수 있음 → 문제 발생!

ThreadLocal을 쓰면:
- 쓰레드마다 자기만의 Connection을 가짐 → 서로 영향 없음!

---

## 3. 프로젝트 세팅

본격적으로 코드를 작성하기 전에 프로젝트의 뼈대를 잡습니다.

### 3-1. 폴더 구조 만들기

아래 구조로 폴더와 빈 파일을 미리 만들어 놓습니다.

```
src/
├── main/java/com/back/
│   ├── Article.java              ← 곧 작성할 파일
│   └── simpleDb/
│       ├── SimpleDb.java         ← 곧 작성할 파일
│       └── Sql.java              ← 곧 작성할 파일
└── test/java/com/back/
    └── simpleDb/
        └── SimpleDbTest.java     ← 미리 제공된 테스트 파일
```

### 3-2. build.gradle.kts 작성

프로젝트에 필요한 **외부 라이브러리(의존성)**를 선언하는 파일입니다.  
각 의존성이 왜 필요한지 주석으로 설명합니다.

```kotlin
plugins {
    id("java")
}

group = "com"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Lombok: getter/setter 등 반복 코드를 어노테이션으로 자동 생성
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // JUnit 5: 테스트 프레임워크
    // junit-bom 대신 Spring Boot가 버전을 관리하도록 두고, launcher는 명시적으로 추가
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

tasks.test {
    useJUnitPlatform()
}
```

> **각 라이브러리 역할 요약**
> - `lombok` → `@Data` 어노테이션으로 getter/setter 자동 생성
> - `mysql-connector-j` → JDBC로 MySQL에 접속할 때 필수
> - `jackson-databind` → DB 조회 결과(Map)를 Article 객체로 변환할 때 사용
> - `jackson-datatype-jsr310` → `LocalDateTime` 같은 Java 8 날짜 타입 처리

### 3-3. Spring Initializr로 생성한 프로젝트 정리 (해당자만)

Spring Initializr(start.spring.io)로 프로젝트를 생성했다면 `BackApplication.java`가 자동으로 만들어집니다.  
이 파일은 Spring Boot를 사용하지 않는 이 프로젝트에서 컴파일 오류를 일으키므로 삭제합니다.

```bash
rm src/main/java/com/back/BackApplication.java
```

> **오류 증상**: `package org.springframework.boot does not exist` 또는 `cannot find symbol: @SpringBootApplication`

### 3-4. JDK 버전 문제 해결 (JDK 25 사용자만 해당)

현재 사용 중인 JDK가 25인 경우 Gradle Kotlin DSL 컴파일이 실패합니다.

**문제 증상**
```
FAILURE: Build failed with an exception.
* What went wrong:
25
```

**해결 방법**: 프로젝트 루트에 `gradle.properties` 파일을 만들어 Java 21을 직접 지정합니다.

```properties
# gradle.properties
org.gradle.java.home=/Users/이름/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home
```

> Java 21이 설치된 경로를 직접 입력하세요.  
> `/usr/libexec/java_home -V` 명령으로 설치된 JDK 목록과 경로를 확인할 수 있습니다.

---

### ✅ 챕터 3 확인하기

세팅이 올바른지 컴파일로 검증합니다.

```bash
./gradlew compileJava
```

**기대 결과**

```
BUILD SUCCESSFUL
```

**문제가 생기면?**

| 증상 | 원인 | 해결 |
|------|------|------|
| `What went wrong: 25` | JDK 25 문제 | gradle.properties에 Java 21 경로 추가 |
| `package org.springframework.boot does not exist` | BackApplication.java 미삭제 | `rm src/main/java/com/back/BackApplication.java` |
| `OutputDirectoryProvider not available` | JUnit 버전 불일치 | junit-bom 제거 후 `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` 추가 |
| `Could not resolve com.mysql...` | 인터넷 연결 없음 | 네트워크 확인 후 재시도 |
| `Could not find or load main class` | 폴더 구조 오류 | src/main/java/com/back 구조 재확인 |

`BUILD SUCCESSFUL`이 출력되면 챕터 4로 넘어갑니다.

---

## 4. Article 클래스 구현

### 역할

DB에서 조회한 게시글 데이터를 담는 그릇입니다.  
데이터베이스의 `article` 테이블과 1:1 대응됩니다.

### article 테이블 구조 (나중에 테스트에서 자동 생성됨)

```sql
CREATE TABLE article (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY(id),
    createdDate DATETIME NOT NULL,
    modifiedDate DATETIME NOT NULL,
    title VARCHAR(100) NOT NULL,
    body TEXT NOT NULL,
    isBlind BIT(1) NOT NULL DEFAULT 0
)
```

### Article.java 구현

```java
package com.back;

import lombok.Data;
import java.time.LocalDateTime;

@Data  // Lombok: getter, setter, toString, equals, hashCode 자동 생성
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;
    private boolean isBlind;
}
```

### @Data 어노테이션이 자동으로 만들어 주는 것

| 필드 | 자동 생성되는 메서드 |
|------|---------------------|
| `long id` | `getId()`, `setId(long)` |
| `String title` | `getTitle()`, `setTitle(String)` |
| `boolean isBlind` | `isBlind()`, `setBlind(boolean)` |
| `LocalDateTime createdDate` | `getCreatedDate()`, `setCreatedDate(LocalDateTime)` |

> `boolean` 타입 필드는 getter 이름이 `get` 대신 `is`로 시작합니다.  
> `isBlind` 필드의 getter → `isBlind()`

### DB 컬럼 ↔ 자바 필드 대응표

| DB 컬럼 (SQL 타입) | 자바 필드 (Java 타입) |
|--------------------|----------------------|
| `id INT UNSIGNED` | `long id` |
| `createdDate DATETIME` | `LocalDateTime createdDate` |
| `modifiedDate DATETIME` | `LocalDateTime modifiedDate` |
| `title VARCHAR(100)` | `String title` |
| `body TEXT` | `String body` |
| `isBlind BIT(1)` | `boolean isBlind` |

---

### ✅ 챕터 4 확인하기

#### 1단계: 컴파일 확인

```bash
./gradlew compileJava
```

**기대 결과**

```
BUILD SUCCESSFUL
```

#### 2단계: Lombok이 getter를 제대로 만들었는지 확인

빌드 후 아래 경로에 컴파일된 `.class` 파일이 생깁니다.  
`javap` 명령으로 Lombok이 생성한 메서드 목록을 확인할 수 있습니다.

```bash
javap -p build/classes/java/main/com/back/Article.class
```

**기대 결과** (일부 발췌)

```
public long getId()
public void setId(long)
public java.lang.String getTitle()
public void setTitle(java.lang.String)
public boolean isBlind()       ← boolean 타입은 is 접두사
public void setBlind(boolean)
```

`isBlind()` getter와 `setBlind(boolean)` setter가 보이면 정상입니다.

> `javap`가 없으면 IntelliJ에서 `Article.java`를 열고  
> `article.` 을 입력했을 때 자동완성에 `isBlind()`, `getId()` 등이 보이면 OK입니다.

`BUILD SUCCESSFUL`이 출력되면 챕터 5로 넘어갑니다.

---

## 5. SimpleDb 클래스 구현

### 역할

- DB 연결(Connection)을 관리합니다
- **ThreadLocal**을 사용해 쓰레드마다 독립적인 Connection을 제공합니다
- 트랜잭션(commit/rollback)을 제어합니다

### 환경 세팅 — MySQL 실행

SimpleDb는 실제로 MySQL에 연결합니다.  
코드 작성 전에 MySQL 컨테이너를 먼저 실행합니다.

> **주의**: 맥에서는 `-v` 옵션 없이 실행해야 TRUNCATE 문제가 없습니다.

```bash
docker run \
    --name mysql-1 \
    -p 3306:3306 \
    -e TZ=Asia/Seoul \
    -e MYSQL_ROOT_PASSWORD=root123414 \
    -d \
    mysql
```

컨테이너가 완전히 뜰 때까지 약 15초 기다렸다가 연결이 되는지 확인합니다.

```bash
docker exec mysql-1 mysql -uroot -proot123414 -e "SELECT 1;"
```

```
+---+
| 1 |
+---+
| 1 |
+---+
```

이 출력이 나오면 MySQL이 정상적으로 실행 중입니다.

---

### 단계별 구현

#### 5-1. 기본 뼈대 만들기

```java
package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private boolean devMode;  // true면 실행 SQL을 콘솔에 출력

    // 핵심: 쓰레드별로 독립적인 Connection을 저장하는 ThreadLocal
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String database) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    boolean isDevMode() {
        return devMode;
    }
}
```

#### 5-2. getConnection() 메서드 — 커넥션 가져오기

이 메서드가 SimpleDb의 핵심입니다.  
현재 쓰레드에 커넥션이 없으면 새로 만들고, 있으면 기존 것을 재사용합니다.

```java
Connection getConnection() {
    // 현재 쓰레드의 커넥션을 꺼냅니다
    Connection conn = connectionHolder.get();

    if (conn == null) {
        // 커넥션이 없으면 새로 만듭니다
        try {
            String url = "jdbc:mysql://" + host + "/" + database
                    + "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
            conn = DriverManager.getConnection(url, user, password);

            // 현재 쓰레드에 커넥션을 저장합니다
            connectionHolder.set(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 실패: " + e.getMessage(), e);
        }
    }

    return conn;  // 기존 커넥션을 재사용
}
```

**ThreadLocal 동작 시각화**

```
[쓰레드 A]                          [쓰레드 B]
getConnection() 호출                getConnection() 호출
connectionHolder.get() → null       connectionHolder.get() → null
새 Connection 생성 (connA)          새 Connection 생성 (connB)
connectionHolder.set(connA)         connectionHolder.set(connB)

다시 getConnection() 호출           다시 getConnection() 호출
connectionHolder.get() → connA ✓   connectionHolder.get() → connB ✓
(재사용)                            (재사용, A의 connA와 무관)
```

#### 5-3. run() 메서드 — DDL/DML 바로 실행

테이블 생성, TRUNCATE 등 결과가 필요 없는 SQL을 실행할 때 씁니다.

```java
public void run(String sql, Object... params) {
    Sql sqlObj = genSql();         // Sql 빌더 객체 생성
    sqlObj.append(sql, params);    // SQL과 파라미터 설정
    sqlObj.execute();              // 실행
}
```

**사용 예시**

```java
// 파라미터 없는 경우
simpleDb.run("DROP TABLE IF EXISTS article");
simpleDb.run("TRUNCATE article");

// 파라미터 있는 경우
simpleDb.run("INSERT INTO article SET title = ?, body = ?", "제목", "내용");
```

#### 5-4. genSql() 메서드 — Sql 빌더 생성

```java
public Sql genSql() {
    // 자기 자신(SimpleDb)을 넘겨서 Sql이 getConnection()을 호출할 수 있게 함
    return new Sql(this);
}
```

#### 5-5. 트랜잭션 메서드들

트랜잭션이란 여러 SQL을 **하나의 묶음**으로 처리하는 것입니다.  
중간에 실패하면 `rollback()`으로 모두 취소, 성공하면 `commit()`으로 확정합니다.

```
startTransaction() → SQL 실행 → commit()   : 전부 반영
startTransaction() → SQL 실행 → rollback() : 전부 취소
```

```java
public void startTransaction() {
    try {
        // autoCommit false → 이제부터 commit() 전까지 DB에 바로 반영 안 됨
        getConnection().setAutoCommit(false);
    } catch (SQLException e) {
        throw new RuntimeException("트랜잭션 시작 실패", e);
    }
}

public void commit() {
    try {
        Connection conn = getConnection();
        conn.commit();            // DB에 최종 반영
        conn.setAutoCommit(true); // 다시 자동 커밋 모드로
    } catch (SQLException e) {
        throw new RuntimeException("커밋 실패", e);
    }
}

public void rollback() {
    try {
        Connection conn = getConnection();
        conn.rollback();          // 변경사항 모두 취소
        conn.setAutoCommit(true); // 다시 자동 커밋 모드로
    } catch (SQLException e) {
        throw new RuntimeException("롤백 실패", e);
    }
}
```

#### 5-6. close() 메서드 — 현재 쓰레드 커넥션 닫기

```java
public void close() {
    Connection conn = connectionHolder.get();
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("커넥션 종료 실패", e);
        } finally {
            // ThreadLocal에서 반드시 제거 (메모리 누수 방지)
            connectionHolder.remove();
        }
    }
}
```

> `connectionHolder.remove()`를 꼭 해줘야 합니다.  
> ThreadLocal을 사용한 후 제거하지 않으면 메모리 누수가 발생합니다.

### SimpleDb.java 최종 코드

```java
package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private boolean devMode;

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String database) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            try {
                String url = "jdbc:mysql://" + host + "/" + database
                        + "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
                conn = DriverManager.getConnection(url, user, password);
                connectionHolder.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException("DB 연결 실패: " + e.getMessage(), e);
            }
        }
        return conn;
    }

    public void run(String sql, Object... params) {
        Sql sqlObj = genSql();
        sqlObj.append(sql, params);
        sqlObj.execute();
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 실패", e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("커밋 실패", e);
        }
    }

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("롤백 실패", e);
        }
    }

    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException("커넥션 종료 실패", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }
}
```

---

### ✅ 챕터 5 확인하기

#### 1단계: 컴파일 확인

```bash
./gradlew compileJava
```

**기대 결과**

```
BUILD SUCCESSFUL
```

#### 2단계: MySQL 실제 연결 확인

`Sql.java`가 아직 없어서 테스트는 실행할 수 없습니다.  
대신 아래 임시 코드를 `SimpleDb.java`에 잠깐 추가해서 연결을 직접 눈으로 확인합니다.

> **주의**: `SimpleDb.java`의 `genSql()`이 `Sql` 타입을 반환하므로 `Sql.java`가 없으면 컴파일 자체가 실패합니다.  
> main 메서드를 추가하기 전에 먼저 아래 빈 껍데기 파일을 만들어야 합니다.
>
> `src/main/java/com/back/simpleDb/Sql.java`
> ```java
> package com.back.simpleDb;
>
> public class Sql {
>     Sql(SimpleDb simpleDb) {}
> }
> ```
> 챕터 6에서 이 파일을 완성합니다.

`SimpleDb.java` 아무 위치에 아래 `main` 메서드를 추가합니다.

```java
// 확인 후 반드시 삭제할 임시 코드
public static void main(String[] args) throws Exception {
    SimpleDb simpleDb = new SimpleDb("localhost", "root", "root123414", "simpleDb__test");

    java.sql.Connection conn = simpleDb.getConnection();
    System.out.println("1) 연결 성공: " + !conn.isClosed());
    System.out.println("2) autoCommit: " + conn.getAutoCommit());

    simpleDb.startTransaction();
    System.out.println("3) 트랜잭션 후 autoCommit: " + conn.getAutoCommit());

    simpleDb.rollback();
    System.out.println("4) 롤백 후 autoCommit: " + conn.getAutoCommit());

    simpleDb.close();
    System.out.println("5) close 후 연결 상태: " + conn.isClosed());
}
```

IntelliJ에서 `main` 메서드 옆의 ▶ 버튼을 클릭해 실행합니다.

**기대 출력**

```
1) 연결 성공: true
2) autoCommit: true
3) 트랜잭션 후 autoCommit: false
4) 롤백 후 autoCommit: true
5) close 후 연결 상태: true
```

**각 출력의 의미**

| 출력 | 의미 |
|------|------|
| `연결 성공: true` | MySQL에 정상 연결됨 |
| `autoCommit: true` | 초기 상태는 자동 커밋 |
| `트랜잭션 후 autoCommit: false` | `startTransaction()`이 autoCommit을 false로 바꿈 |
| `롤백 후 autoCommit: true` | `rollback()` 후 다시 자동 커밋으로 복원 |
| `close 후 연결 상태: true` | `close()` 후 커넥션이 닫힘 |

확인이 끝났으면 `main` 메서드를 **반드시 삭제**하고 챕터 6으로 넘어갑니다.

> **연결 실패 시 확인 사항**
> - `docker ps`로 mysql-1 컨테이너가 Up 상태인지 확인
> - 비밀번호가 `root123414`인지 확인
> - `simpleDb__test` 데이터베이스가 존재하는지 확인

---

## 6. Sql 클래스 구현

### 역할

- SQL 문장을 조각조각 이어 붙이는 **빌더(Builder)** 역할
- 완성된 SQL을 실제로 실행하는 **실행기** 역할
- DB 결과(ResultSet)를 자바 객체로 변환하는 **변환기** 역할

### 개발 방식 — 테스트 하나씩 통과시키며 커밋

테스트를 하나씩 통과시키면서 구현하고, 통과할 때마다 커밋합니다.

```
테스트 실패 확인 → 최소 코드 구현 → 테스트 통과 확인 → 커밋 → 다음 테스트
```

---

### 6-0. 뼈대 만들기 + 초기 커밋

먼저 `src/main/java/com/back/simpleDb/Sql.java`를 아래 내용으로 만들고 커밋합니다.

```java
package com.back.simpleDb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    // Map → 자바 객체 변환기
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // getter 대신 필드 이름을 직접 사용 → isBlind 매핑 문제 해결
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }
}
```

> **ObjectMapper 필드 가시성 설정 이유**  
> Lombok이 `boolean isBlind` 필드에서 `isBlind()` getter를 만들면  
> Jackson은 프로퍼티명을 `blind`로 인식합니다.  
> `FIELD` 가시성으로 바꾸면 `isBlind`로 올바르게 매핑됩니다.
>
> ```
> DB 컬럼명:             isBlind
> Jackson 기본 인식:     blind   ← 불일치! 매핑 실패
> Jackson 필드 기반:     isBlind ← 일치! 매핑 성공
> ```

```bash
git add -A && git commit -m "feat: Sql 뼈대 추가"
```

---

### 6-1. t001 — insert

**테스트 실행 (실패 확인)**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t001"
```

**구현 — `append()`, `prepare()`, `execute()`, `insert()`**

```java
public Sql append(String sql, Object... appendParams) {
    if (sqlBuilder.length() > 0) {
        sqlBuilder.append("\n");
    }
    sqlBuilder.append(sql);
    Collections.addAll(params, appendParams);
    return this;  // 메서드 체이닝용
}

private PreparedStatement prepare(Connection conn) throws SQLException {
    String sql = sqlBuilder.toString();

    if (simpleDb.isDevMode()) {
        System.out.println("== rawSql ==");
        System.out.println(sql);
        System.out.println("params: " + params);
    }

    // RETURN_GENERATED_KEYS → INSERT 후 생성된 id를 받기 위해 필요
    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));  // JDBC 인덱스는 1부터!
    }
    return ps;
}

void execute() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn)) {
        ps.execute();
    } catch (SQLException e) {
        throw new RuntimeException("SQL 실행 실패: " + e.getMessage(), e);
    }
}

public long insert() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn)) {
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    } catch (SQLException e) {
        throw new RuntimeException("INSERT 실패: " + e.getMessage(), e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t001"
```

```
SimpleDbTest > insert PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql insert 구현 (t001 통과)"
```

---

### 6-2. t002 — update

**구현 — `update()`**

```java
public int update() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn)) {
        return ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException("UPDATE 실패: " + e.getMessage(), e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t002"
```

```
SimpleDbTest > update PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql update 구현 (t002 통과)"
```

---

### 6-3. t003 — delete

`delete()`는 `update()`와 내부적으로 동일합니다.

**구현 — `delete()`**

```java
public int delete() {
    return update();
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t003"
```

```
SimpleDbTest > delete PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql delete 구현 (t003 통과)"
```

---

### 6-4. t004, t005 — selectRows, selectRow (Map 반환)

**구현 — `convertValue()`, `selectRows()`, `selectRow()`**

DB에서 꺼낸 값은 자바 타입과 달라 변환이 필요합니다.

| DB 타입 | JDBC `getObject()` 반환 | 변환 후 |
|---------|------------------------|---------|
| `DATETIME` | `Timestamp` | `LocalDateTime` |
| `BIT(1)` | `Boolean` 또는 `byte[]` | `Boolean` |

```java
private Object convertValue(Object value) {
    if (value instanceof Timestamp) {
        return ((Timestamp) value).toLocalDateTime();
    }
    if (value instanceof byte[]) {
        byte[] bytes = (byte[]) value;
        return bytes.length > 0 && bytes[0] != 0;
    }
    return value;
}

public List<Map<String, Object>> selectRows() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {

        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                String colName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(colName, convertValue(value));
            }
            rows.add(row);
        }

        return rows;
    } catch (SQLException e) {
        throw new RuntimeException("SELECT 실패: " + e.getMessage(), e);
    }
}

public Map<String, Object> selectRow() {
    List<Map<String, Object>> rows = selectRows();
    return rows.isEmpty() ? null : rows.get(0);
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t004" \
               --tests "com.back.simpleDb.SimpleDbTest.t005"
```

```
SimpleDbTest > selectRows PASSED
SimpleDbTest > selectRow PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectRows/selectRow(Map) 구현 (t004, t005 통과)"
```

---

### 6-5. t006 — selectDatetime

**구현 — `selectDatetime()`**

```java
public LocalDateTime selectDatetime() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            Timestamp ts = rs.getTimestamp(1);
            return ts != null ? ts.toLocalDateTime() : null;
        }
        return null;
    } catch (SQLException e) {
        throw new RuntimeException("selectDatetime 실패", e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t006"
```

```
SimpleDbTest > selectDatetime PASSED
```

> **실패 시**: JDBC URL에 `serverTimezone=Asia/Seoul`이 있는지 확인하세요.

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectDatetime 구현 (t006 통과)"
```

---

### 6-6. t007 — selectLong

**구현 — `selectLong()`**

```java
public Long selectLong() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getLong(1);
        return null;
    } catch (SQLException e) {
        throw new RuntimeException("selectLong 실패", e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t007"
```

```
SimpleDbTest > selectLong PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectLong 구현 (t007 통과)"
```

---

### 6-7. t008 — selectString

**구현 — `selectString()`**

```java
public String selectString() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getString(1);
        return null;
    } catch (SQLException e) {
        throw new RuntimeException("selectString 실패", e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t008"
```

```
SimpleDbTest > selectString PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectString 구현 (t008 통과)"
```

---

### 6-8. t009~t011 — selectBoolean (3개)

**구현 — `selectBoolean()`**

```java
public Boolean selectBoolean() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getBoolean(1);
        return null;
    } catch (SQLException e) {
        throw new RuntimeException("selectBoolean 실패", e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t009" \
               --tests "com.back.simpleDb.SimpleDbTest.t010" \
               --tests "com.back.simpleDb.SimpleDbTest.t011"
```

```
SimpleDbTest > selectBoolean PASSED
SimpleDbTest > selectBoolean, 2nd PASSED
SimpleDbTest > selectBoolean, 3rd PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectBoolean 구현 (t009~t011 통과)"
```

---

### 6-9. t012 — select LIKE 사용법

`append()`로만 구현됩니다. 추가 코드 없이 테스트를 실행합니다.

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t012"
```

```
SimpleDbTest > select, LIKE 사용법 PASSED
```

**커밋**

```bash
git add -A && git commit -m "test: LIKE 사용법 통과 확인 (t012)"
```

---

### 6-10. t013, t014 — appendIn, selectLongs

**구현 — `appendIn()`, `selectLongs()`**

`appendIn()`은 `WHERE id IN (?)` 의 `?` 하나를 여러 개로 자동 확장합니다.

```
appendIn("WHERE id IN (?)", 1, 2, 3)
   → "WHERE id IN (?, ?, ?)"  +  params: [1, 2, 3]
```

```java
public Sql appendIn(String sql, Object... values) {
    List<Object> expanded = new ArrayList<>();
    if (values.length == 1 && values[0] instanceof Object[]) {
        // Long[] 같은 배열이 통째로 넘어온 경우 원소 단위로 풀기
        Collections.addAll(expanded, (Object[]) values[0]);
    } else {
        Collections.addAll(expanded, values);
    }

    String expandedSql = sql.replace("?",
            String.join(", ", Collections.nCopies(expanded.size(), "?")));

    if (sqlBuilder.length() > 0) {
        sqlBuilder.append("\n");
    }
    sqlBuilder.append(expandedSql);
    params.addAll(expanded);

    return this;
}

public List<Long> selectLongs() {
    Connection conn = simpleDb.getConnection();
    try (PreparedStatement ps = prepare(conn);
         ResultSet rs = ps.executeQuery()) {
        List<Long> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getLong(1));
        }
        return result;
    } catch (SQLException e) {
        throw new RuntimeException("selectLongs 실패", e);
    }
}
```

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t013" \
               --tests "com.back.simpleDb.SimpleDbTest.t014"
```

```
SimpleDbTest > appendIn PASSED
SimpleDbTest > selectLongs, ORDER BY FIELD 사용법 PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql appendIn/selectLongs 구현 (t013, t014 통과)"
```

---

### 6-11. t015, t016 — selectRows, selectRow (Article 객체 반환)

**구현 — `selectRows(Class)`, `selectRow(Class)`**

`Map<String, Object>`를 `Article` 같은 자바 클래스 인스턴스로 변환합니다.

```
Map {"id": 1L, "title": "제목1", "isBlind": false, ...}
    ↓  objectMapper.convertValue(row, Article.class)
Article { id=1, title="제목1", isBlind=false, ... }
```

```java
public <T> List<T> selectRows(Class<T> clazz) {
    List<Map<String, Object>> rows = selectRows();
    List<T> result = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
        result.add(objectMapper.convertValue(row, clazz));
    }
    return result;
}

public <T> T selectRow(Class<T> clazz) {
    Map<String, Object> row = selectRow();
    if (row == null) return null;
    return objectMapper.convertValue(row, clazz);
}
```

> **`isBlind` 값이 항상 false로 나온다면**  
> ObjectMapper의 `setVisibility(PropertyAccessor.FIELD, ...)` 설정을 6-0에서 빠뜨린 것입니다.

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t015" \
               --tests "com.back.simpleDb.SimpleDbTest.t016"
```

```
SimpleDbTest > selectRows, Article PASSED
SimpleDbTest > selectRow, Article PASSED
```

**커밋**

```bash
git add -A && git commit -m "feat: Sql selectRows/selectRow(Class) 구현 (t015, t016 통과)"
```

---

### 6-12. t017~t019 — 멀티스레딩, 트랜잭션

t017~t019는 `SimpleDb`에 이미 구현된 `ThreadLocal`, `startTransaction()`, `commit()`, `rollback()`을 테스트합니다. `Sql`에 추가할 코드가 없습니다.

**테스트 통과 확인**

```bash
./gradlew test --tests "com.back.simpleDb.SimpleDbTest.t017" \
               --tests "com.back.simpleDb.SimpleDbTest.t018" \
               --tests "com.back.simpleDb.SimpleDbTest.t019"
```

```
SimpleDbTest > use in multi threading PASSED
SimpleDbTest > rollback PASSED
SimpleDbTest > commit PASSED
```

**전체 테스트 최종 확인**

```bash
./gradlew test
```

```
19 tests completed, 0 failed
```

**커밋**

```bash
git add -A && git commit -m "test: 전체 19개 테스트 통과"
```

---


### Sql.java 최종 코드

```java
package com.back.simpleDb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;

    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... appendParams) {
        if (sqlBuilder.length() > 0) {
            sqlBuilder.append("\n");
        }
        sqlBuilder.append(sql);
        Collections.addAll(params, appendParams);
        return this;
    }

    public Sql appendIn(String sql, Object... values) {
        List<Object> expanded = new ArrayList<>();
        if (values.length == 1 && values[0] instanceof Object[]) {
            Collections.addAll(expanded, (Object[]) values[0]);
        } else {
            Collections.addAll(expanded, values);
        }

        String expandedSql = sql.replace("?",
                String.join(", ", Collections.nCopies(expanded.size(), "?")));

        if (sqlBuilder.length() > 0) {
            sqlBuilder.append("\n");
        }
        sqlBuilder.append(expandedSql);
        params.addAll(expanded);

        return this;
    }

    private PreparedStatement prepare(Connection conn) throws SQLException {
        String sql = sqlBuilder.toString();

        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(sql);
            System.out.println("params: " + params);
        }

        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        return ps;
    }

    void execute() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 실패: " + e.getMessage(), e);
        }
    }

    public long insert() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("INSERT 실패: " + e.getMessage(), e);
        }
    }

    public int update() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UPDATE 실패: " + e.getMessage(), e);
        }
    }

    public int delete() {
        return update();
    }

    private Object convertValue(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            return bytes.length > 0 && bytes[0] != 0;
        }
        return value;
    }

    public List<Map<String, Object>> selectRows() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {

            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(colName, convertValue(value));
                }
                rows.add(row);
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("SELECT 실패: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Long selectLong() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("selectLong 실패", e);
        }
    }

    public List<Long> selectLongs() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {
            List<Long> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("selectLongs 실패", e);
        }
    }

    public String selectString() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("selectString 실패", e);
        }
    }

    public Boolean selectBoolean() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBoolean(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("selectBoolean 실패", e);
        }
    }

    public LocalDateTime selectDatetime() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("selectDatetime 실패", e);
        }
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rows = selectRows();
        List<T> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(objectMapper.convertValue(row, clazz));
        }
        return result;
    }

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        if (row == null) return null;
        return objectMapper.convertValue(row, clazz);
    }
}
```

---

## 7. 테스트 실행

### 환경 세팅 — 테스트용 DB 생성

테스트는 `simpleDb__test` 데이터베이스를 사용합니다.  
아직 만들지 않았다면 지금 생성합니다.

```bash
docker exec mysql-1 mysql -uroot -proot123414 -e "CREATE DATABASE IF NOT EXISTS simpleDb__test;"
```

오류 없이 종료되면 성공입니다.

> 테스트 코드의 `createArticleTable()` 메서드가 `article` 테이블을 자동으로 생성하므로  
> DB만 만들어 두면 됩니다.

### 전체 테스트 실행

```bash
./gradlew test
```

### ✅ 챕터 7 확인하기 — 19개 전체 통과

**기대 결과**

```
SimpleDbTest > insert PASSED
SimpleDbTest > update PASSED
SimpleDbTest > delete PASSED
SimpleDbTest > selectRows PASSED
SimpleDbTest > selectRow PASSED
SimpleDbTest > selectDatetime PASSED
SimpleDbTest > selectLong PASSED
SimpleDbTest > selectString PASSED
SimpleDbTest > selectBoolean PASSED
SimpleDbTest > selectBoolean, 2nd PASSED
SimpleDbTest > selectBoolean, 3rd PASSED
SimpleDbTest > select, LIKE 사용법 PASSED
SimpleDbTest > appendIn PASSED
SimpleDbTest > selectLongs, ORDER BY FIELD 사용법 PASSED
SimpleDbTest > selectRows, Article PASSED
SimpleDbTest > selectRow, Article PASSED
SimpleDbTest > use in multi threading PASSED
SimpleDbTest > rollback PASSED
SimpleDbTest > commit PASSED

19 tests completed, 0 failed
```

**일부 테스트가 실패한다면?**

| 실패 테스트 | 확인할 곳 |
|-------------|-----------|
| t001 insert | `insert()` 메서드, `RETURN_GENERATED_KEYS` |
| t002 update | `update()` 메서드의 `executeUpdate()` 반환값 |
| t004~t005 | `convertValue()`의 Timestamp → LocalDateTime 변환 |
| t006 datetime | JDBC URL의 `serverTimezone=Asia/Seoul` |
| t009~t011 boolean | `selectBoolean()`의 `rs.getBoolean(1)` |
| t013~t014 appendIn | `appendIn()`의 `?` 확장 로직 |
| t015~t016 Article | ObjectMapper의 `FIELD` visibility 설정 |
| t017 threading | `ThreadLocal<Connection>` 사용 여부 |
| t018~t019 | `startTransaction()` / `commit()` / `rollback()` |

---

## 8. 자주 하는 실수

### 실수 1: JDBC 인덱스는 0이 아니라 1부터 시작

```java
// 틀림
ps.setObject(0, value);  // SQLException 발생!

// 맞음
ps.setObject(1, value);  // 첫 번째 ?
ps.setObject(2, value);  // 두 번째 ?
```

### 실수 2: ResultSet, PreparedStatement 닫지 않기

DB 자원을 닫지 않으면 커넥션이 고갈됩니다.

```java
// 나쁜 예
PreparedStatement ps = conn.prepareStatement(sql);
ResultSet rs = ps.executeQuery();
// 닫는 코드 없음 → 자원 누수!

// 좋은 예 — try-with-resources 사용
try (PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // ...
}  // 블록 종료 시 rs.close(), ps.close() 자동 호출
```

### 실수 3: ThreadLocal 사용 후 remove() 안 하기

```java
// 나쁜 예
connectionHolder.set(conn);
conn.close();
// remove 없이 종료 → 메모리 누수, 다음 요청에서 닫힌 커넥션 재사용 위험

// 좋은 예
try {
    conn.close();
} finally {
    connectionHolder.remove();  // 반드시 제거!
}
```

### 실수 4: macOS에서 MySQL 도커에 -v 옵션 사용

```bash
# 틀림 — TRUNCATE 오류 발생 가능
docker run -v /my/data:/var/lib/mysql ... mysql

# 맞음 — -v 없이 실행
docker run --name mysql-1 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=... -d mysql
```

### 실수 5: append 파라미터를 배열로 넘기기

```java
Object[] values = {"제목", "내용"};

// 틀림 — values 자체가 하나의 Object로 취급됨
sql.append("INSERT ... VALUES (?, ?)", values);

// 맞음 — 직접 나열
sql.append("INSERT ... VALUES (?, ?)", "제목", "내용");
```

---

## 핵심 개념 요약

| 개념 | 한 줄 설명 |
|------|-----------|
| JDBC | Java에서 DB와 대화하는 표준 인터페이스 |
| Connection | DB와의 연결 세션 |
| PreparedStatement | `?` 자리표시를 사용하는 안전한 SQL 실행기 |
| ResultSet | SQL 실행 결과를 행(row) 단위로 읽는 커서 |
| ThreadLocal | 쓰레드마다 독립적인 변수를 제공하는 컨테이너 |
| 트랜잭션 | 여러 SQL을 하나의 묶음으로 처리 (commit/rollback) |
| Jackson ObjectMapper | Map ↔ 자바 객체 변환기 |
| 빌더 패턴 | 메서드를 체이닝해서 객체를 조립하는 패턴 |
