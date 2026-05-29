# SimpleDb

## 개요
JDBC를 직접 다루는 경험을 통해 JPA, MyBatis 같은 ORM 프레임워크의 동작 원리를 이해하기 위해 구현한 경량 DB 유틸리티 라이브러리입니다.

## 주요 기능

### SimpleDb
- DB 연결 및 Sql 객체 생성 (`genSql()`)
- DDL/DML 직접 실행 (`run()`)
- 트랜잭션 관리 (`startTransaction()`, `commit()`, `rollback()`)
- `ThreadLocal`을 통한 멀티스레드 안전한 Connection 관리

### Sql
- 메서드 체이닝 방식의 SQL 빌더 (`append()`, `appendIn()`)
- 다양한 결과 타입 반환
    - `insert()` → long (생성된 id)
    - `update()`, `delete()` → int (영향받은 row 수)
    - `selectRow()`, `selectRows()` → Map 또는 제네릭 DTO
    - `selectLong()`, `selectString()`, `selectBoolean()`, `selectDatetime()`
    - `selectLongs()` → List\<Long\>

## 어려웠던 점

### 1. JDBC API를 직접 다루는 것 자체가 낯설었다
평소에 JPA나 MyBatis를 사용하면 SQL 실행 결과가 자동으로 객체에 담겨서 나오는데, 
JDBC는 `Connection`, `PreparedStatement`, `ResultSet`을 직접 열고 닫고 값을 꺼내야 했다. 
특히 `rs.next()`를 호출하지 않으면 데이터를 읽을 수 없다는 것을 몰라서 에러가 처음 발생했을 때 원인을 찾는 데 시간이 걸렸다.

### 2. DB 타입과 Java 타입이 다르다는 점
DB의 `DATETIME`은 JDBC에서 `Timestamp`로 반환되어 `LocalDateTime`으로 변환이 필요했고, 
`BIT(1)`은 드라이버에 따라 `Boolean`이나 숫자로 반환되어 별도 처리가 필요했다. 
DB 타입과 Java 타입이 1:1로 매핑되지 않는다는 점이 처음에는 이해하기 어려웠다.

### 3. 리플렉션으로 DB 결과를 객체에 매핑하는 것
`selectRows(Article.class)`처럼 어떤 클래스가 들어올지 모르는 상황에서 DB 컬럼명과 Java 필드명을 매핑해야 했다.
`getDeclaredField()`, `setAccessible()`, `field.set()` 같은 리플렉션 API를 처음 접하는 입장에서는 개념 자체가 낯설었고, 
필드명을 약간 틀려 사소하게 오류들이 발생해 찾아내는데 시간이 걸린 경우가 좀 있었다.

## 느낀 점
- JDBC를 직접 구현하면서 JPA, MyBatis가 내부적으로 얼마나 많은 것을 자동으로 처리해주는지 체감할 수 있었다.
- `ThreadLocal`을 통한 Connection 관리가 Spring의 `@Transactional` 내부 동작과 같은 원리라는 점이 인상적이었다.
- 단순히 동작 여부만 확인하는 것이 아니라 왜 동작하는지를 이해하는 것이 중요하다는 것을 느꼈다.