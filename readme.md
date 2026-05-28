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
