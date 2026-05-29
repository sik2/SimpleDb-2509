<body>
<!--StartFragment--><html><head></head><body><h1>SimpleDb 구현 &amp; 테스트 회고</h1>
<h2>1. 구현 목표</h2>
<p>Spring 없이 순수 JDBC만으로 Spring JdbcTemplate처럼 편하게 DB를 쓸 수 있는<br>
<code>SimpleDb</code> 유틸리티 클래스를 직접 구현하고 19개의 테스트를 통과시키는 것.</p>
<hr>
<h2>2. 구현한 파일 구조</h2>
<pre><code>src/main/java/com/back/
├── Article.java                  ← 결과를 담는 DTO
└── simpleDb/
    ├── SimpleDb.java             ← DB 연결 및 트랜잭션 관리
    └── Sql.java                  ← 쿼리 빌더 (체이닝 방식)
</code></pre>
<hr>
<h2>3. 핵심 구현 내용</h2>
<h3>SimpleDb.java</h3>

메서드 | 역할
-- | --
getConnection() | ThreadLocal로 스레드별 Connection 관리
run() | DDL/DML 직접 실행
genSql() | Sql 빌더 객체 생성
startTransaction() | autoCommit false로 트랜잭션 시작
commit() | 커밋 후 autoCommit true 복원
rollback() | 롤백 후 autoCommit true 복원
close() | ThreadLocal Connection 반납 및 제거


<hr>
<h2>5. 실패 원인과 해결</h2>
<h3>❌ 실패 1 — Spring import 충돌</h3>
<p><strong>원인</strong></p>
<pre><code class="language-java">// 테스트 파일에 잘못된 import가 있었음
import org.springframework.test.context.jdbc.Sql; // @interface (어노테이션)

// 근데 테스트에서 클래스처럼 쓰고 있었음
Sql sql = simpleDb.genSql(); // 컴파일 에러
</code></pre>
<p><strong>해결</strong></p>
<pre><code class="language-java">// 해당 import 한 줄 제거 → 자동으로 com.back.simpleDb.Sql 인식
</code></pre>
<hr>
<h3>❌ 실패 2 — t006 타임존 이슈 (32400초 차이)</h3>
<p><strong>원인</strong></p>
<pre><code>Docker MySQL          → UTC 기준으로 NOW() 반환 (예: 05:00:00)
LocalDateTime.now()   → KST 기준              (예: 14:00:00)
차이                  → 9시간 = 32,400초 💥
</code></pre>
<p><strong>시도 1 실패</strong> — <code>rs.getObject(LocalDateTime.class)</code></p>
<pre><code class="language-java">// 타임존 변환 없이 그대로 읽어서 여전히 9시간 차이
return rs.getObject(1, LocalDateTime.class);
</code></pre>
<p><strong>시도 2 실패</strong> — <code>serverTimezone=UTC</code> URL 설정만 변경</p>
<pre><code class="language-java">// URL만 바꿔서는 selectDatetime() 내부 변환 로직이 그대로라 해결 안 됨
</code></pre>
<p><strong>최종 해결</strong> — UTC Calendar로 명시적으로 읽고 KST로 변환</p>
<pre><code class="language-java">Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
Timestamp ts = rs.getTimestamp(1, utcCal);
return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
</code></pre>
<pre><code>UTC Timestamp 읽기  →  Instant(절대시간)  →  시스템 타임존(KST) 적용  →  일치 ✅
</code></pre>
<hr>
<h2>6. 배운 점</h2>
<h3>JDBC 핵심 흐름 이해</h3>
<pre><code>Connection → PreparedStatement → (executeQuery/executeUpdate) → ResultSet
</code></pre>
<p>Spring이 내부에서 이 흐름을 자동으로 처리해주고 있었다는 걸 직접 구현하며 체감.</p>
<h3>ThreadLocal의 필요성</h3>
<p>멀티스레딩 환경에서 Connection 하나를 공유하면 동시성 문제가 발생.<br>
<code>ThreadLocal&lt;Connection&gt;</code>으로 스레드마다 독립된 Connection을 가지게 해서 해결.</p>
<h3>ResultSet 타입 매핑</h3>
<p>DB 컬럼 타입과 Java 타입이 자동으로 맞춰지지 않기 때문에<br>
<code>ResultSetMetaData.getColumnType()</code>으로 타입을 직접 분기 처리해야 함.</p>
<h3>타임존은 명시적으로 처리해야 한다</h3>
<p><code>Timestamp.toLocalDateTime()</code>은 타임존 변환을 하지 않음.<br>
DB 서버와 JVM의 타임존이 다를 경우 반드시 <code>Instant</code> → <code>ZonedDateTime</code> 변환 필요.</p>
<h3>리플렉션으로 DTO 매핑</h3>
<p>Jackson 없이도 <code>Field.setAccessible(true)</code>와 타입 변환 로직으로<br>
Map → DTO 매핑이 가능. 단, Lombok의 boolean 필드명(<code>isBlind</code>) 처리에 주의.</p>
<h3>appendIn 설계</h3>
<p><code>IN (?, ?, ?)</code> 처럼 가변 개수의 파라미터를 받는 경우<br>
<code>?</code> 하나를 동적으로 N개로 확장하는 로직이 필요.</p>
<hr>
<h2>7. 미비사항 및 개선 포인트</h2>
<h3>Connection Pool 미적용</h3>
<p>현재는 매 요청마다 새 Connection을 생성하는 구조.<br>
실무에서는 HikariCP 같은 커넥션 풀을 사용해야 성능이 나옴.</p>
<h3>예외 처리 미흡</h3>
<p>모든 SQLException을 <code>RuntimeException</code>으로 감싸서 던지고 있어<br>
어떤 SQL에서 어떤 이유로 실패했는지 파악하기 어려움.</p>
<h3>NULL 처리 미완성</h3>
<p><code>selectRow()</code>가 결과 없을 때 <code>null</code>을 반환하는데<br>
호출부에서 NPE가 날 수 있어 Optional 반환을 고려할 수 있음.</p>
<h3>devMode rawSql 출력 불완전</h3>
<p>현재 <code>?</code>를 단순 문자열 치환으로 출력하기 때문에<br>
실제 PreparedStatement가 실행하는 SQL과 100% 동일하지 않음.</p>
<h3>타입 지원 범위 제한</h3>
<p>현재 <code>mapRow()</code>에서 지원하는 타입이 제한적.<br>
DATE, TIME, DECIMAL, JSON 등 추가 타입 대응이 필요.</p></body></html><!--EndFragment-->
<h3>깃허브 커밋</h3>
<p>그냥 코드를 무작정 짜고 테스트하고 반복하다보니 중간마다 커밋해주는걸 자꾸 잊어먹어서 다음부턴 커밋을 잊지말고 해야겠다고 생각했음.<br></p>
</body>
</html>