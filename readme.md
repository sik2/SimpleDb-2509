## README

# SimpleDb

순수 JDBC로 구현한 경량 DB 유틸리티

## 개발 환경
- Java 21, Gradle
- mysql-connector-j 9.3.0
- JUnit 5, AssertJ
- Lombok
## 프로젝트 구조
src/
├── main/java/com/back/
│   ├── Article.java              # DTO
│   └── simpleDb/
│       ├── SimpleDb.java         # DB 연결 및 트랜잭션 관리
│       └── Sql.java              # SQL 빌더 및 실행
└── test/java/com/back/simpleDb/
└── SimpleDbTest.java         # 단위 테스트 t001~t019


---

## 단위 테스트 체크리스트

- [x] t001 - insert
- [x] t002 - update
- [x] t003 - delete
- [x] t004 - selectRows
- [x] t005 - selectRow
- [x] t006 - selectDatetime
- [x] t007 - selectLong
- [x] t008 - selectString
- [x] t009 - selectBoolean
- [x] t010 - selectBoolean 2nd
- [x] t011 - selectBoolean 3rd
- [x] t012 - LIKE 사용법
- [x] t013 - appendIn
- [x] t014 - selectLongs, ORDER BY FIELD
- [x] t015 - selectRows, Article 매핑
- [x] t016 - selectRow, Article 매핑
- [x] t017 - 멀티스레딩
- [x] t018 - rollback
- [x] t019 - commit

### 💡 배운 점

- `ThreadLocal`로 스레드별 커넥션을 독립적으로 관리하는 방법
- `PreparedStatement`로 SQL 인젝션 없이 파라미터 바인딩하는 방법
- 리플렉션으로 ResultSet → DTO 자동 매핑하는 방법
- 메서드 체이닝을 위해 `return this` 패턴 사용

### 🤔 어려웠던 점

- `ThreadLocal` 개념 이해 (스레드별로 독립적인 값을 가진다는 것)
- `insert()`와 `update()`의 차이 (`RETURN_GENERATED_KEYS`, `getGeneratedKeys()`)
- `appendIn()`에서 `?` 하나를 값 개수만큼 동적으로 확장해야 한다는 것
