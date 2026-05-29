# 1. 테스트 코드 기준 과제 방향 요약

## 질문 요약

이 과제가 `자료구조로 직접 데이터를 저장하는 DBMS`를 만드는 것인지, 아니면 다른 방식인지 정리해달라는 질문이다.

## 핵심 결론

이 과제는 `자체 자료구조 DBMS`가 아니라 `MySQL용 SimpleDb 유틸`을 만드는 과제다.

데이터는 자바 컬렉션에 저장하는 것이 아니라 `실제 DB 테이블(article)`에 저장한다.

## 자료구조는 어디에 쓰는가

자료구조는 `데이터 저장`이 아니라 `SQL 실행 보조`에 쓴다.

1. `StringBuilder`
   SQL 문자열 조립
2. `List<Object>`
   `?` 파라미터 저장
3. `Map<String, Object>`
   조회 결과 한 행 저장
4. `ThreadLocal<Connection>`
   스레드별 DB 커넥션 보관

## 테스트가 요구하는 기능

1. `run(...)`
   SQL 직접 실행
2. `genSql()`
   SQL 빌더 생성
3. `append(...)`, `appendIn(...)`
   SQL과 파라미터 누적
4. `insert()`, `update()`, `delete()`
5. `selectLong()`, `selectString()`, `selectBoolean()`, `selectDatetime()`
6. `selectRow()`, `selectRows()`
7. `selectRow(Article.class)`, `selectRows(Article.class)`
8. `startTransaction()`, `commit()`, `rollback()`
9. 멀티스레드 환경에서 스레드별 독립 커넥션 사용

## 구현 방향

핵심은 이것이다.

`SimpleDb 객체는 하나를 공유하고, Connection 은 스레드마다 따로 사용한다.`

그래서 구현은 보통 아래 순서로 하면 된다.

1. `SimpleDb`에 DB 접속 정보 저장
2. `ThreadLocal<Connection>` 으로 스레드별 커넥션 관리
3. `run(...)` 구현
4. `Sql` 클래스 구현
5. 조회 결과를 `Map` 또는 `Article` 객체로 변환
6. 트랜잭션 기능 추가

## 한 줄 정리

이 과제는 `자료구조 기반 미니 DBMS`가 아니라 `멀티스레드 안전한 JDBC 기반 SimpleDb`를 만드는 과제다.

# 2. `run()`이 DDL을 그대로 전달하는지, 인자를 동적으로 받을 수 있는지

## 질문 요약

`createArticleTable()` 안의 DDL 쿼리를 `run()`이 그대로 DB에 전달하면 되는지, 그리고 SQL 파라미터 인자의 개수를 고정하지 않고 동적으로 받을 수 있는지에 대한 질문이다.

## 핵심 설명

`run()`은 전달받은 SQL 문자열을 JDBC로 직접 실행하면 된다.

즉 `CREATE TABLE`, `DROP TABLE`, `TRUNCATE` 같은 DDL도 `run()`이 그대로 DB에 전달해서 실행하면 된다.

다만 이 과제는 `MyBatis`나 `JPA`를 쓰는 방식이 아니라, `SimpleDb`가 직접 JDBC로 처리하는 방식에 가깝다.

## 인자를 동적으로 받는 방법

자바에서는 `가변 인자(varargs)` 문법을 쓰면 된다.

```java
public String run(String sql, Object... args) {
}
```

이렇게 선언하면 인자 개수가 매번 달라도 받을 수 있다.

```java
run("TRUNCATE article");
run("SELECT * FROM article WHERE id = ?", 1);
run("INSERT INTO article SET title = ?, body = ?, isBlind = ?", "제목", "내용", false);
```

## 구현 흐름

1. SQL 문자열 받기
2. `args`를 순서대로 꺼내기
3. `PreparedStatement`에 순서대로 바인딩하기
4. SQL 실행하기

## 한 줄 정리

`run()`은 DDL도 그대로 JDBC로 실행하면 되고, 인자는 `Object... args`로 받아서 동적으로 처리하면 된다.

# 3. `Object... args`로 인자를 동적으로 받는 방식

## 질문 요약

오버로딩 대신 `Object... args`를 사용해서 SQL 인자를 동적으로 받는 방식에 대한 정리다.

## 핵심 설명

자바에서는 `Object... args`를 사용하면 인자의 개수를 고정하지 않고 받을 수 있다.

```java
public String run(String sql, Object... args) {
}
```

이 방식이면 아래처럼 인자 개수가 달라도 메서드 하나로 처리할 수 있다.

```java
run("TRUNCATE article");
run("SELECT * FROM article WHERE id = ?", 1);
run("INSERT INTO article SET title = ?, body = ?, isBlind = ?", "제목", "내용", false);
```

## 왜 이 방식이 좋은가

1. SQL마다 인자 개수가 달라도 대응 가능하다.
2. 오버로딩을 여러 개 만들 필요가 없다.
3. `PreparedStatement`에 순서대로 바인딩하기 쉽다.

## 한 줄 정리

`Object... args`는 SQL 파라미터를 개수 제한 없이 동적으로 받기 위한 가장 간단한 방식이다.

# 4. `appendIn()`에서 `?`를 여러 개로 늘리는 방식

## 질문 요약

`String questionMarks = "?";`, `Collections.nCopies()`, `String.join()`, `query.replace()`가 어떻게 연결되어 `IN (?)` 쿼리를 처리하는지에 대한 질문이다.

## 핵심 설명

`appendIn("WHERE id IN (?)", 1, 2, 3)`에서 SQL 문자열에는 `?`가 1개뿐이다.

하지만 실제로는 값이 3개이므로 JDBC가 바인딩할 자리도 3개가 필요하다.

그래서 `?` 1개를 `?, ?, ?`로 바꿔야 한다.

```java
String questionMarks = "?";

if (args.length > 0) {
    questionMarks = String.join(", ", Collections.nCopies(args.length, "?"));
}

query = query.replace("?", questionMarks);

stringBuilder.append(query).append("\n");

for (Object arg : args) {
    params.add(arg);
}
```

## 코드 흐름

1. `questionMarks`의 기본값은 `"?"`이다.
2. `args.length`가 3이면 `Collections.nCopies(3, "?")`가 `["?", "?", "?"]`를 만든다.
3. `String.join(", ", ...)`이 리스트를 `"?, ?, ?"` 문자열로 합친다.
4. `query.replace("?", questionMarks)`가 기존 쿼리의 `?`를 `"?, ?, ?"`로 바꾼다.
5. `params`에는 실제 값인 `1`, `2`, `3`을 순서대로 넣는다.

## 예시

처음 입력:

```java
appendIn("WHERE id IN (?)", 1, 2, 3)
```

변환된 SQL:

```sql
WHERE id IN (?, ?, ?)
```

저장된 params:

```java
[1, 2, 3]
```

그 후 `PreparedStatement`가 params를 순서대로 바인딩해서 DB에는 `id IN (1, 2, 3)` 조건처럼 동작한다.

## 한 줄 정리

`appendIn()`은 params 값을 쿼리에 직접 붙이는 것이 아니라, params 개수만큼 `?` 자리를 늘리고 실제 값은 `PreparedStatement`가 바인딩하게 만드는 방식이다.
