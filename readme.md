# SimpleDb Troubleshooting

`SimpleDb`와 `Sql`을 구현하면서 헷갈리기 쉬운 지점을 문제 상황별로 정리합니다.

각 항목은 `증상 -> 원인 -> 해결 -> 주의` 흐름으로 읽으면 됩니다.

## `handler.handle(stmt)`가 실제 SQL 실행 함수처럼 보이는 문제

증상:
`handler.handle(stmt)`가 DB에 직접 등록하거나 SQL을 실행하는 핵심 함수처럼 보입니다.

원인:
`handler.handle(stmt)`는 실행 자체를 직접 정의하지 않고, 밖에서 넘겨받은 실행 방식을 호출합니다.

```java
return handler.handle(stmt);
```

예를 들어 `run()`은 `PreparedStatement::executeUpdate`를 넘깁니다.

```java
public void run(String sql, Object... params) {
    execute(sql, params, PreparedStatement::executeUpdate);
}
```

해결:
`handler.handle(stmt)`는 “실행 위임 지점”으로 이해하면 됩니다.

실제 실행은 아래처럼 넘겨진 코드가 결정합니다.

```text
PreparedStatement::executeUpdate -> INSERT, UPDATE, DELETE 실행
stmt -> stmt.executeQuery()      -> SELECT 실행
```

주의:
`handler.handle(stmt)`를 막으면 SQL 실행 전체가 막힙니다. 따라서 `devMode` 같은 옵션으로 무작정 호출을 막으면 반환값이 필요한 메서드가 깨질 수 있습니다.

## `devMode`로 실제 SQL 실행을 막고 싶은 문제

증상:
개발 모드에서는 SQL을 DB에 보내지 않고 확인만 하고 싶습니다.

원인:
현재 구조에서는 `handler.handle(stmt)`가 실제 실행까지 이어지므로, 여기서 실행을 막으면 `insert()`, `update()`, `selectRows()`의 반환값을 만들 수 없습니다.

해결:
`devMode`는 실행 차단보다 SQL 출력용으로 쓰는 편이 안전합니다.

```java
if (devMode) {
        System.out.println(sql);
    System.out.println(Arrays.toString(params));
        }
```

주의:
실제로 DB에 보내지 않는 기능이 필요하면 `devMode`와 분리해서 `dryRun` 같은 별도 옵션을 두는 편이 좋습니다. `dryRun`을 만들 때는 `insert`, `update`, `select` 각각에 어떤 기본 반환값을 줄지도 정해야 합니다.

## `Can't call rollback when autocommit=true`

증상:
`rollback()` 호출 시 아래 예외가 발생합니다.

```text
java.sql.SQLNonTransientConnectionException: Can't call rollback when autocommit=true
```

원인:
`rollback()`을 호출한 `Connection`이 `autoCommit=false` 상태가 아닙니다.

가장 흔한 원인은 `startTransaction()`에서 사용한 커넥션과 `insert()`, `rollback()`에서 사용한 커넥션이 서로 다른 경우입니다.

트랜잭션은 반드시 같은 `Connection` 안에서 실행되어야 합니다.

```java
simpleDb.startTransaction();

simpleDb.genSql()
        .append("INSERT INTO article")
        .append("SET createdDate = NOW(), modifiedDate = NOW(), title = ?, body = ?", "title", "body")
        .insert();

simpleDb.rollback();
```

해결:
`startTransaction()`, SQL 실행, `commit()` 또는 `rollback()`이 같은 스레드의 같은 커넥션을 사용하게 해야 합니다.

`ThreadLocal<Connection>`을 쓰면 현재 스레드별로 커넥션을 유지할 수 있습니다.

주의:
`startTransaction()`에서 `Connection`을 `try-with-resources`에 넣으면 메서드가 끝날 때 커넥션이 닫힙니다.

```java
try (Connection conn = getConnection()) {
        conn.setAutoCommit(false);
}
```

이렇게 되면 이후 `rollback()`에서 새 커넥션을 열게 되고, 새 커넥션은 기본값이 `autoCommit=true`라서 예외가 발생합니다.

## 트랜잭션 중 커넥션이 너무 빨리 닫히는 문제

증상:
`startTransaction()`을 호출했는데도 이후 쿼리가 트랜잭션에 묶이지 않습니다.

원인:
`execute()`나 `startTransaction()`에서 `Connection`을 자동으로 닫고 있을 가능성이 큽니다.

```java
try (Connection conn = getConnection()) {
        // ...
        }
```

해결:
`PreparedStatement`는 짧게 닫아도 되지만, 트랜잭션 중인 `Connection`은 `commit()` 또는 `rollback()`까지 유지해야 합니다.

```java
Connection conn = getConnection();

try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        // ...
        }
```

주의:
일반 쿼리에서는 커넥션을 계속 열어두면 안 됩니다. 트랜잭션 중이 아닐 때는 실행 후 `close()`로 정리해야 합니다.

## `ThreadLocal.remove()`가 꼭 필요한지 헷갈리는 문제

증상:
`conn.close()`를 했는데도 `connectionThread.remove()`가 꼭 필요한지 헷갈립니다.

원인:
두 코드는 역할이 다릅니다.

```text
conn.close()                 -> DB 연결을 닫음
connectionThread.remove()    -> 현재 스레드에 저장된 참조를 제거
```

해결:
`ThreadLocal<Connection>`을 쓴다면 `finally`에서 `remove()`를 호출하는 편이 안전합니다.

```java
finally {
        connectionThread.remove();
}
```

주의:
스레드풀 환경에서는 스레드가 재사용됩니다. `ThreadLocal` 값을 지우지 않으면 닫힌 커넥션이나 이전 작업의 상태가 다음 작업에 남을 수 있습니다.

## `SimpleDb`와 `Sql`의 책임이 섞이는 문제

증상:
어떤 코드를 `SimpleDb`에 둬야 하고, 어떤 코드를 `Sql`에 둬야 하는지 헷갈립니다.

원인:
두 클래스가 모두 SQL과 관련되어 있지만, 책임의 방향이 다릅니다.

해결:
역할을 아래처럼 나누면 단순해집니다.

```text
SimpleDb = Connection 관리, PreparedStatement 생성, 파라미터 바인딩, 트랜잭션 처리
Sql      = SQL 문자열 조립, append/appendIn 처리, select 결과 변환
```

따라서 `getConnection`, `setParams`, `execute`, `commit`, `rollback`은 `SimpleDb`에 두는 것이 자연스럽습니다.

반대로 `selectRows`, `selectRow`, `selectLong`, `selectString`, `selectBoolean`처럼 결과를 어떤 형태로 받을지 결정하는 코드는 `Sql`에 두는 편이 읽기 쉽습니다.

주의:
`Sql`이 직접 `DriverManager.getConnection(...)`을 호출하기 시작하면 역할이 섞입니다. `Sql`은 실행을 직접 관리하기보다 `SimpleDb.execute(...)`를 이용하는 쪽이 좋습니다.

## 실행 메서드마다 중복 코드가 늘어나는 문제

증상:
`insert`, `update`, `delete`, `select`를 각각 만들다 보면 아래 코드가 계속 반복됩니다.

```text
Connection 가져오기
PreparedStatement 생성
파라미터 바인딩
SQLException 처리
```

원인:
SQL 실행 준비 과정은 거의 같은데, 실행 후 반환 타입만 다르기 때문입니다.

```text
update/delete -> int
insert        -> long
selectRows    -> List<Map<String, Object>>
selectRow     -> Map<String, Object>
```

해결:
공통 실행 흐름은 `SimpleDb.execute(...)`에 모으고, 달라지는 실행 방식만 `handler`로 넘깁니다.

```java
<T> T execute(
        String sql,
        Object[] params,
        StatementHandler<T> handler
) {
    Connection conn = getConnection();

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        setParams(stmt, params);

        return handler.handle(stmt);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
```

`<T>`를 사용하면 `execute(...)`의 반환 타입을 호출하는 쪽에서 결정할 수 있습니다.

```java
public int runForRowsCount(String sql, Object... params) {
    return execute(sql, params, PreparedStatement::executeUpdate);
}
```

효과:
`Connection`, `PreparedStatement`, 파라미터 바인딩 코드를 여러 메서드에 반복해서 쓰지 않아도 됩니다.

주의:
`<T>`는 타입을 자동 변환해주는 기능이 아닙니다. 실제 반환값은 `handler` 안에서 작성한 코드가 결정합니다.

## `ResultSet`을 객체로 바꾸는 코드가 특정 클래스에 묶이는 문제

증상:
처음에는 `Article`만 조회하면 되지만, 나중에 다른 테이블 객체가 생기면 `selectArticles`, `selectMembers` 같은 메서드가 계속 늘어날 수 있습니다.

원인:
`ResultSet`을 어떤 클래스의 객체로 만들지 실행 시점에 알 수 없으면, 클래스마다 별도 매핑 코드를 작성해야 합니다.

해결:
`Class<T>`를 메서드 인자로 받아서 어떤 타입으로 만들지 호출하는 쪽에서 넘깁니다.

```java
List<Article> articleRows = sql.selectRows(Article.class);
```

내부에서는 전달받은 `Class<T>`로 객체를 생성합니다.

```java
T instance = cls.getDeclaredConstructor().newInstance();
```

그리고 컬럼명과 같은 이름의 필드에 값을 넣습니다.

```java
String columnName = metaData.getColumnName(i);
Object value = rs.getObject(i);

try {
var field = cls.getDeclaredField(columnName);
    field.setAccessible(true);
    field.set(instance, value);
} catch (NoSuchFieldException ignored) {
        }
```

효과:
`Article.class`, `Member.class`처럼 필드명이 컬럼명과 맞는 클래스라면 같은 `selectRows(Class<T> cls)` 메서드를 재사용할 수 있습니다.

주의:
Java의 제네릭 타입 `T`는 런타임에 사라지므로 `new T()`를 직접 할 수 없습니다. 실제 객체를 만들려면 `Article.class` 같은 타입 정보가 필요합니다.
