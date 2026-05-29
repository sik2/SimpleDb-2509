# SimpleDb 구현

## 구현 기능 목록

- `SimpleDb`: JDBC 커넥션 관리, 트랜잭션, 즉시 실행(`run()`)
- `Sql`: SQL 빌더, DML 실행, 단일값/행 조회, DTO 매핑
- `Article`: DTO 클래스

## 커밋 히스토리

TDD 방식으로 t001부터 t019까지 테스트를 하나씩 통과시키며 구현했습니다.

| 테스트 | 구현 내용 |
|--------|----------|
| t001 | `append()`, `insert()` |
| t002 | `update()` |
| t003 | `delete()` |
| t004 | `selectRows()`, `extractValue()` |
| t005 | `selectRow()` |
| t006 | `selectDatetime()` |
| t007 | `selectLong()` |
| t008 | `selectString()` |
| t009~t011 | `selectBoolean()` |
| t012~t013 | `appendIn()` |
| t014 | `selectLongs()` |
| t015~t019 | DTO 매핑, 멀티스레딩, 트랜잭션 |

## 💡 어려웠던 점 & 느낀 점

### ThreadLocal 커넥션 관리
멀티스레드 환경에서 `SimpleDb` 인스턴스 하나를 여러 스레드가 공유할 때,
각 스레드가 독립적인 커넥션을 가져야 한다는 점이 처음엔 낯설었다.
`ThreadLocal<Connection>`을 사용하면 스레드마다 별도의 값을 유지할 수 있다는 것을 배웠고,
동기화 없이도 스레드 안전한 코드를 작성할 수 있다는 점이 인상적이었다.

### appendIn 구현
`appendIn("WHERE id IN (?)", 1, 2, 3)`처럼 단일 `?`를 여러 개로 확장하는 방식이
처음엔 어떻게 구현해야 할지 감이 잡히지 않았다.
`replaceFirst("\\?", placeholders)`로 해결할 수 있다는 걸 알게 되었고,
정규식을 실제 문제에 적용해보는 좋은 경험이 됐다.

### 순수 JDBC의 번거로움
Spring의 `JdbcTemplate`이나 JPA에 익숙해져 있다 보니,
순수 JDBC로 `PreparedStatement`, `ResultSet`, `ResultSetMetaData`를
직접 다루는 것이 처음엔 번거롭게 느껴졌다.
하지만 덕분에 평소에 프레임워크가 대신 해주던 일들이 무엇인지
더 깊이 이해할 수 있었다.