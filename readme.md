```

## Troubleshooting

### `handler.handle(stmt)`가 실제 SQL 실행 지점인가?

`handler.handle(stmt)` 자체가 특정 SQL을 실행하는 함수는 아닙니다.

`SimpleDb.execute(...)`에서 `PreparedStatement`를 만들고 파라미터를 바인딩한 뒤, 실제 실행 방법을 `handler`에게 넘깁니다.

```java
return handler.handle(stmt);
```

예를 들어 `run()`은 `PreparedStatement::executeUpdate`를 넘기기 때문에 `INSERT`, `UPDATE`, `DELETE`, `CREATE TABLE` 같은 SQL이 실제로 실행됩니다.

```java
public void run(String sql, Object... params) {
    execute(sql, params, PreparedStatement::executeUpdate);
}
```

즉 `handler.handle(stmt)`는 공통 실행 지점이고, 실제 동작은 넘겨진 람다나 메서드 참조가 결정합니다.

### `devMode`로 SQL 실행을 막아도 되는가?

가능은 하지만 권장하지 않습니다.

`devMode`에서 `handler.handle(stmt)` 호출을 막으면 `insert()`, `update()`, `selectRows()`처럼 반환값이 필요한 메서드가 깨질 수 있습니다.

`devMode`는 실제 실행 차단보다 SQL과 파라미터를 출력하는 디버깅 용도로 쓰는 편이 안전합니다.

```java
if (devMode) {
    System.out.println(sql);
    System.out.println(Arrays.toString(params));
}
```

실제로 DB에 보내지 않는 기능이 필요하다면 `devMode`와 분리해서 `dryRun` 같은 별도 옵션으로 만드는 편이 좋습니다.

### `Can't call rollback when autocommit=true`

이 에러는 `rollback()`을 호출한 `Connection`이 `autoCommit=false` 상태가 아닐 때 발생합니다.

가장 흔한 원인은 `startTransaction()`에서 만든 커넥션과 `insert()`, `rollback()`에서 쓰는 커넥션이 서로 다른 경우입니다.

트랜잭션은 반드시 같은 `Connection` 안에서 실행되어야 합니다.

```java
simpleDb.startTransaction();

simpleDb.genSql()
        .append("INSERT INTO article")
        .append("SET createdDate = NOW(), modifiedDate = NOW(), title = ?, body = ?", "title", "body")
        .insert();

simpleDb.rollback();
```

위 흐름에서 `startTransaction()`, `insert()`, `rollback()`은 같은 스레드의 같은 커넥션을 사용해야 합니다.

`try-with-resources`에 `Connection`을 넣으면 메서드가 끝날 때 커넥션이 바로 닫히므로 트랜잭션이 끊길 수 있습니다.

```java
try (Connection conn = getConnection()) {
    conn.setAutoCommit(false);
}
```

위 코드는 `startTransaction()` 종료 시점에 커넥션이 닫히므로 피해야 합니다.

`PreparedStatement`는 짧게 닫아도 되지만, 트랜잭션 중인 `Connection`은 `commit()` 또는 `rollback()`까지 유지해야 합니다.

### `ThreadLocal.remove()`가 필요한가?

`ThreadLocal<Connection>`을 쓴다면 `remove()`는 필요합니다.

`conn.close()`는 DB 연결을 닫는 것이고, `connectionThread.remove()`는 현재 스레드에 남아 있는 커넥션 참조를 지우는 것입니다.

```java
finally {
    connectionThread.remove();
}
```

특히 스레드풀 환경에서는 스레드가 재사용되므로, `ThreadLocal` 값을 지우지 않으면 닫힌 커넥션이나 이전 작업의 상태가 다음 작업에 남을 수 있습니다.

### `SimpleDb`와 `Sql`의 책임 분리

역할은 아래처럼 나누는 편이 가장 단순합니다.

```text
SimpleDb = Connection 관리, PreparedStatement 생성, 파라미터 바인딩, 트랜잭션 처리
Sql      = SQL 문자열 조립, append/appendIn 처리, select 결과 변환
```

따라서 `setParams`, `getConnection`, `commit`, `rollback`, `execute`는 `SimpleDb`에 두는 것이 자연스럽습니다.

반대로 `selectRows`, `selectRow`, `selectLong`, `selectString`, `selectBoolean`처럼 결과를 어떤 형태로 받을지 결정하는 코드는 `Sql`에 두는 것이 더 읽기 쉽습니다.

### 실행 메서드마다 중복 코드가 늘어나는 문제

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

예를 들어 `update/delete`는 `int`를 반환하고, `insert`는 `long`을 반환하고, `selectRows`는 `List<Map<String, Object>>`를 반환합니다.

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

여기서 `<T>`를 사용한 이유는 `execute(...)`의 반환 타입을 호출하는 쪽에서 정하게 하기 위해서입니다.

```java
public int runForRowsCount(String sql, Object... params) {
    return execute(sql, params, PreparedStatement::executeUpdate);
}
```

효과:
`Connection`, `PreparedStatement`, 파라미터 바인딩 코드를 여러 메서드에 반복해서 쓰지 않아도 됩니다.

주의:
`<T>`는 타입을 자동으로 바꿔주는 기능이 아닙니다. 실제로 `int`를 반환할지, `List`를 반환할지는 `handler` 안에서 작성한 코드가 결정합니다.

### `ResultSet`을 객체로 바꾸는 코드가 특정 클래스에 묶이는 문제

증상:
처음에는 `Article`만 조회하면 되지만, 나중에 다른 테이블 객체가 생기면 `selectArticles`, `selectMembers`처럼 비슷한 메서드가 계속 늘어날 수 있습니다.

원인:
`ResultSet`을 어떤 클래스의 객체로 만들지 실행 시점에 알 수 없으면, 클래스마다 별도 매핑 코드를 작성해야 하기 때문입니다.

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
`Class<T>`를 넘기는 이유는 런타임에 `new T()`를 직접 할 수 없기 때문입니다. Java의 제네릭 타입 `T`는 런타임에 사라지므로, 실제 객체를 만들려면 `Article.class` 같은 타입 정보가 필요합니다.
