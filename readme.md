# SimpleDb - 순수 JDBC 경량 DB 유틸리티

## 📌 프로젝트 개요

Spring, JPA 등 프레임워크 없이 **순수 JDBC**만으로 경량 DB 유틸리티를 직접 구현한 프로젝트입니다.  
멀티스레드 환경에서의 커넥션 관리, 트랜잭션 제어, SQL 빌더, DTO 매핑까지 핵심 기능을 스스로 설계했습니다.

---

## 테스트 결과

| 테스트 | 설명 | 결과 |
|--------|------|------|
| t001 | insert | green |
| t002 | update | green |
| t003 | delete | green |
| t004 | selectRows | green |
| t005 | selectRow | green |
| t006 | selectDatetime | green |
| t007 | selectLong | green |
| t008 | selectString | green |
| t009 | selectBoolean | green |
| t010 | selectBoolean 2nd | green |
| t011 | selectBoolean 3rd | green |
| t012 | LIKE 사용법 | green |
| t013 | appendIn | green |
| t014 | ORDER BY FIELD | green |
| t015 | selectRows Article 매핑 | green |
| t016 | selectRow Article 매핑 | green |
| t017 | 멀티스레딩 | green |
| t018 | rollback | green |
| t019 | commit | green |

---

```
src
├── main
│   └── java/com/back
│       ├── SimpleDb.java   # 커넥션 관리, 트랜잭션, SQL 실행
│       ├── Sql.java        # SQL 빌더, 조회/실행 메서드
│       └── Article.java    # DTO
└── test
    └── java/com/back
        └── SimpleDbTest.java
```

---

## 핵심 구현

### 1. ThreadLocal 커넥션 관리

`SimpleDb` 인스턴스 하나를 여러 스레드가 공유해도 안전하도록  
`ThreadLocal<Connection>`으로 스레드별 독립 커넥션을 유지했습니다.

```java
private final ThreadLocal<Connection> threadLocalConn = new ThreadLocal<>();

public Connection getConnection() throws SQLException {
    Connection conn = threadLocalConn.get();
    if (conn == null || conn.isClosed()) {
        conn = DriverManager.getConnection(url, username, password);
        threadLocalConn.set(conn);
    }
    return conn;
}

public void close() {
    Connection conn = threadLocalConn.get();
    if (conn != null) conn.close();
    threadLocalConn.remove();
}
```

### 2. SQL 빌더

메서드 체이닝으로 SQL을 조립하고, `?` 바인딩으로 SQL Injection을 방지합니다.

```java
simpleDb.genSql()
    .append("SELECT * FROM article")
    .append("WHERE id = ?", 1)
    .selectRow();
```

`appendIn()`은 IN 절의 `?` 하나를 인자 개수만큼 자동 확장합니다.

```java
.appendIn("WHERE id IN (?)", 1, 2, 3)
// → WHERE id IN (?, ?, ?)
```

### 3. 트랜잭션

동일 스레드 내 같은 커넥션을 재사용하므로 트랜잭션 경계가 정확히 보장됩니다.

```java
simpleDb.startTransaction(); // autoCommit=false
simpleDb.genSql()...insert();
simpleDb.rollback();         // autoCommit=true 복구
```

### 4. DTO 매핑

리플렉션으로 DB 컬럼명과 클래스 필드명을 매핑합니다.  
MySQL 타입(`DATETIME`, `BIT`)도 Java 타입(`LocalDateTime`, `Boolean`)으로 자동 변환합니다.

```java
Article article = simpleDb.genSql()
    .append("SELECT * FROM article WHERE id = 1")
    .selectRow(Article.class);
```

---

## 어려웠던 점 & 느낀 점

### 커넥션 관리의 복잡함
처음에는 `getConnection()`이 매번 새 커넥션을 만들었습니다.  
트랜잭션은 같은 커넥션에서 이루어져야 하는데, 매번 새 커넥션을 열면 `startTransaction()`과 `insert()`가 서로 다른 커넥션을 쓰게 되어 트랜잭션이 동작하지 않았습니다.  
커넥션을 재사용하되, 멀티스레드 환경에서는 `ThreadLocal`로 스레드별로 격리해야 한다는 것을 직접 겪으면서 배웠습니다.

### 메서드 오버로딩 함정
`run(String sql)`과 `run(String sql, Object... args)` 두 시그니처가 공존할 때,  
인자가 없는 호출은 가변인자가 아닌 단일 인자 메서드로 분기되어 `return null`만 실행되는 버그가 있었습니다.  
가변인자 하나로 통일하는 것이 맞다는 것을 깨달았습니다.

### JPA가 해주는 것들
DATETIME→LocalDateTime, BIT→Boolean 타입 변환, 리플렉션 매핑 등  
평소 JPA가 자동으로 처리해주던 것들을 직접 구현하면서  
프레임워크 내부에서 얼마나 많은 일이 일어나고 있는지 체감했습니다.

### 중복 코드 제거
`insert()`, `update()`, `delete()`가 각자 커넥션을 열고 닫는 코드를 반복하고 있었습니다.  
`execute(boolean)`으로 DML을 공통화하고, `executeQuery()`로 SELECT를 공통화하면서  
중복을 줄이는 리팩토링이 얼마나 코드를 읽기 쉽게 만드는지 실감했습니다.
