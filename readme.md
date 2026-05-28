# SimpleDb

## 개요

JDBC를 직접 다루는 경험을 통해 JPA, MyBatis 같은 ORM 프레임워크의 동작 원리를 이해하기 위해 구현한 경량 DB 유틸리티 라이브러리입니다.

`SimpleDb`와 `Sql` 두 클래스로 구성되며, SQL 빌더 패턴과 다양한 결과 타입 반환을 지원합니다.

---

## 목적

- JDBC의 `Connection`, `PreparedStatement`, `ResultSet` 등 핵심 객체의 동작 원리 이해
- JPA, MyBatis가 내부적으로 JDBC를 어떻게 추상화하는지 직접 구현하며 학습
- 트랜잭션, 멀티스레드 환경에서의 Connection 관리 방법 학습
- 리플렉션을 활용한 제네릭 DTO 매핑 구현

---

## 주요 기능

### SimpleDb
- DB 연결 및 `Sql` 객체 생성 (`genSql()`)
- DDL/DML 직접 실행 (`run()`)
- 트랜잭션 관리 (`startTransaction()`, `commit()`, `rollback()`)
- `ThreadLocal`을 통한 멀티스레드 안전한 Connection 관리

### Sql
- 메서드 체이닝 방식의 SQL 빌더 (`append()`, `appendIn()`)
- 다양한 결과 타입 반환
    - `insert()` → `long` (생성된 id)
    - `update()`, `delete()` → `int` (영향받은 row 수)
    - `selectRow()`, `selectRows()` → `Map` 또는 제네릭 DTO
    - `selectLong()`, `selectString()`, `selectBoolean()`, `selectDatetime()`
    - `selectLongs()` → `List<Long>`

---

## 트러블슈팅

### 1. 초기 세팅 - JDBC 연결

`SimpleDb`, `Sql` 초기 세팅 시 JDBC 연결을 위한 핵심 객체들을 파악하였습니다.

- `Connection` → DB와의 연결을 나타내는 객체
- `PreparedStatement` → SQL 실행 및 파라미터 바인딩
- `ResultSet` → 쿼리 결과를 담는 객체

AI를 이용하여 구현하였으며, 이 과정에서 파악한 객체들을 이후 테스트 구현에 활용하였습니다.

---

### 2. insert() - getGeneratedKeys() 사용법

**문제**

`ps.getGeneratedKeys()`에서 `rs.next()` 호출 없이 바로 `getLong(1)`을 호출하여 에러 발생

**원인**

`ResultSet`은 커서가 첫 번째 행 이전에 위치하므로 반드시 `rs.next()`로 이동 후 값을 꺼내야 함

**해결**

```java
var rs = ps.getGeneratedKeys();
if (rs.next()) {
    return rs.getLong(1);
}
```

---

### 3. 롤백 구현 시 트랜잭션 문제

**문제 1 - 매번 새로운 Connection 생성**

`getConnection()`이 매번 새로운 `Connection`을 생성하여 `startTransaction()`과 실제 쿼리가 서로 다른 `Connection`을 사용하게 됨. 따라서 `setAutoCommit(false)` 설정이 유지되지 않아 롤백이 동작하지 않음

**문제 2 - 단일 Connection 공유 시 멀티스레드 충돌**

문제 1을 해결하기 위해 `Connection`을 필드로 빼서 공유하니, 멀티스레드 환경에서 여러 스레드가 같은 `Connection`을 사용하여 충돌 발생

**해결 - ThreadLocal 사용**

스레드마다 독립적인 `Connection`을 유지하면서 같은 스레드 내에서는 항상 같은 `Connection`을 재사용하도록 `ThreadLocal`로 해결

```java
private final ThreadLocal<Connection> connection = new ThreadLocal<>();

private Connection getConnection() throws SQLException {
    if (connection.get() == null) {
        String url = "jdbc:mysql://" + host + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
        connection.set(DriverManager.getConnection(url, user, password));
    }
    return connection.get();
}
```

| 방법 | 트랜잭션 | 멀티스레드 |
|------|---------|-----------|
| 매번 새 Connection | ❌ | ✅ |
| 단일 Connection 필드 | ✅ | ❌ |
| ThreadLocal | ✅ | ✅ |

---

### 4. try-with-resources로 인한 Connection 자동 종료

**문제**

`run()` 에서 `Connection`을 try-with-resources로 관리하니 블록이 끝날 때 자동으로 `close()`가 호출되어 트랜잭션이 사라짐

```
startTransaction() → AutoCommit false 설정
        ↓
run() 실행 → try-with-resources가 Connection.close() 호출
        ↓
Connection 닫힘 → 트랜잭션 사라짐 💥
        ↓
rollback() → 이미 닫힌 Connection에 접근 → 에러
```

**원인**

`Connection`은 `AutoCloseable` 인터페이스를 구현하므로 try-with-resources 블록이 끝날 때 자동으로 `close()`가 호출됨

**해결**

`PreparedStatement`만 try-with-resources로 관리하고, `Connection`은 직접 `close()`로 생명주기를 관리

```java
public void run(String query, Object... params) {
    try (PreparedStatement ps = getConnection().prepareStatement(query)) {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        ps.execute();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
```

---

### 5. 제네릭 DTO 매핑 - 타입 불일치 문제

**문제**

`colValue.getClass()`로 setter를 찾으면 DB 타입과 Java 타입 불일치로 setter를 찾지 못함

- DB의 `Timestamp` vs Java의 `LocalDateTime`
- DB의 `Boolean` vs Java의 `boolean` (primitive)

**해결**

setter를 이름으로 먼저 찾고, setter의 파라미터 타입 기준으로 값을 변환하는 방식으로 해결

```java
Class<?> paramType = setter.getParameterTypes()[0];
if (colValue instanceof Timestamp && paramType == LocalDateTime.class) {
    colValue = ((Timestamp) colValue).toLocalDateTime();
}
```

---

### 6. 제네릭 DTO 매핑 - isBlind 필드 매핑 문제

**문제**

컬럼명 `isBlind`로 setter를 찾을 때 `setIsBlind`를 찾으려 했으나 Lombok `@Setter`는 boolean 타입 필드의 `is` 접두사를 제거하여 `setBlind`로 생성하므로 매핑 실패

테스트 데이터 id 1~3은 `isBlind = false`라 기본값과 같아 **우연히 통과**되었으나 id 4~6 조회 시 실패 확인

**해결**

컬럼명이 `is`로 시작하면 `is`를 제거하여 setter 이름 생성

```java
String setterName = (colName.startsWith("is")) ?
    "set" + colName.substring(2, 3).toUpperCase() + colName.substring(3) :
    "set" + colName.substring(0, 1).toUpperCase() + colName.substring(1);
```

---

### 7. 제네릭 DTO 매핑 - effectively final 문제

**문제**

람다 안에서 외부 변수 `setterName`을 재할당하면 컴파일 에러 발생

```
Variable used in lambda expression should be final or effectively final
```

**원인**

Java 람다는 외부 변수를 캡처할 때 해당 변수가 effectively final이어야 함

**해결**

삼항연산자로 변수에 한번에 할당하여 재할당 없이 처리

```java
String setterName = (colName.startsWith("is")) ?
    "set" + colName.substring(2, 3).toUpperCase() + colName.substring(3) :
    "set" + colName.substring(0, 1).toUpperCase() + colName.substring(1);
```

---

## 회고

이번 구현을 통해 JPA, MyBatis의 내부 동작 원리를 직접 구현하며 이해할 수 있었습니다.

1. `PreparedStatement`의 `?` 바인딩이 MyBatis의 파라미터 바인딩과 동일한 원리임을 확인하였습니다.
2. `ResultSet`을 DTO로 매핑하는 과정이 JPA의 엔티티 매핑과 같은 원리임을 확인하였습니다.
3. `ThreadLocal`을 활용한 Connection 관리가 Spring의 `@Transactional` 내부 동작과 동일한 방식임을 확인했습니다.
4. 리플렉션으로 제네릭 매핑을 구현하며 ORM이 어떻게 다양한 엔티티를 자동 매핑하는지 이해하였습니다.
5. 여러 트러블슈팅 경험을 통해 단순히 동작 여부만 확인하는 것이 아니라, isBlind처럼 우연히 통과되는 케이스를 의심하고 검증하는 습관의 중요성을 깨달았습니다.
6. 실제 JDBC 코드를 작성하며 ORM 프레임워크가 얼마나 많은 편의 기능과 복잡한 로직을 추상화해주는지 체감할 수 있었습니다.