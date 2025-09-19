## 🚩 과제 목표
- 순수 **JDBC**로 경량 DB 유틸리티(**SimpleDb**)를 구현한다.
- **멀티스레드 환경**(예: Spring WebMVC)에서 안전하게 동작하는 **커넥션 관리**를 설계한다.
- **트랜잭션(Commit/Rollback)**, **SQL 빌더**, **DTO/엔티티 매핑** 등 핵심 기능을 스스로 설계/구현한다.
- 제공된 **단위 테스트(SimpleDbTest)** 전 항목 `통과(✅ t001~t019)`를 최종 목표로 한다.

## ✅ 주요 학습 포인트
### 함수형 인터페이스
- 함수형 인터페이스를 사용해 람다를 인자로 넘겨, 중복 코드를 하나의 메소드에서 처리할 수 있도록 구현
- `@FunctionalInterface` 어노테이션을 사용하여 함수형 인터페이스를 커스텀
    ```
    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }
    ```
### Map vs ThreadLocal
- 멀티 스레드 환경에서의 접근을 허용하기 위해 팀 내에 위 두 가지 시도가 있었음
- Map 사용 시 동시성 문제를 해결하기 위해 Synchronize 처리를 별도로 해줘야 했고, ThreadLocal 혹은 `Concurrency Hash Map`을 사용하는 게 더 효율적일 수 있다는 의견을 도출
    ```
    private final ThreadLocal<Connection> myConn = new ThreadLocal<>();
    ...
    ...
    public Connection getConnection() {
        try {
            Connection conn = myConn.get();
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(this.url, this.user, this.password);
                myConn.set(conn);
            }
    
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("DB Connection 중 예외 발생", e);
        }
    }
    ```
  
### AutoClosable 구현
- SimpleDb에 AutoClosable 인터페이스의 close 메소드를 오버라이드하여 `try-with-resources`를 사용할 수 있게 함
    ```
    public class SimpleDb implements AutoCloseable {
    ...
    ...
    @Override
    public void close() {
        try {
            if (myConn.get() != null) {
                myConn.get().close();
                myConn.remove();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 종료 중 연결 에러", e);
        }
    }
    ```
## 💬 어려웠던 점 & 느낀 점
- **김찬종**  
  - 과제를 진행하고 팀원들의 코드를 리뷰하며 다른 구현 방식이나 개선점을 찾을 수 있었습니다.  
  - 함수형 인터페이스나 리플렉션을 활용해 코드의 재사용성과 확장성을 높일 수 있는 방법에 대해서도 고민해볼 수 있었습니다.

- **정다솔**  
  - 평소에는 서로 코드에 리뷰만 남겼는데, 이번에는 발표까지 진행하며 의견을 바로 나눌 수 있어서 좋았습니다.  
  - 또한 다른 팀원의 코드를 통해 몰랐던 Java 문법이나 사용법을 익힐 수 있어 유익한 시간이었습니다.

- **김도하**  
  - 발표와 코드 리뷰를 통해 팀원분들의 의견과 생각을 들으며 더 발전할 수 있는 계기가 되었습니다.  
  - 다만 주목받는 상황에서 긴장해 머릿속이 하얘지는 부분은 극복해야 할 과제라고 느꼈습니다.

- **홍석환**  
  - 다양한 코드를 접할 수 있어서 좋았고, 리팩토링하면서 구현하지 못했던 부분이 아쉬웠습니다.  
  - 하지만 함수형 인터페이스를 활용한 팀원의 구현을 보고 저도 시도해보고 싶다는 생각이 들었습니다.

- **장근영**  
  - 개념적으로만 알던 트랜잭션을 직접 단계별로 구현하며 체득할 수 있었던 프로젝트였습니다.  
  - 코드 리뷰를 통해 놓친 부분을 발견할 수 있었고, 다양한 아이디어를 보며 생각을 넓게 가져야겠다고 느꼈습니다.

- **이승원**  
  - 코드 리뷰를 통해 다른 분들의 피드백을 받을 수 있었던 것이 큰 도움이 되었습니다.  
  - 발표를 하며 텍스트보다 말로 주고받는 피드백이 훨씬 와닿는다는 걸 깨달았습니다.  
  - 또한 내가 얼핏 아는 내용을 다른 사람에게 설명하는 것이 생각보다 쉽지 않다는 점을 느끼며 보완해야겠다고 생각했습니다.
