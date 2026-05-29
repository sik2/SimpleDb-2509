package com.back.simpleDb;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql implements AutoCloseable {

    private final SimpleDb simpleDb;
    private final StringBuilder queryBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... args) {
        if (!queryBuilder.isEmpty()) {
            queryBuilder.append(" ");
        }
        queryBuilder.append(sqlPart);
        if (args != null) {
            params.addAll(Arrays.asList(args));
        }
        return this;
    }

    public Sql appendIn(String sqlPart, Object... args) {
        if (args.length == 1 && args[0] instanceof Long[]) {
            args = (Long[]) args[0];
        } else if (args.length == 1 && args[0] instanceof Object[]) {
            args = (Object[]) args[0];
        }

        StringBuilder inPlaceholders = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            inPlaceholders.append("?");
            if (i < args.length - 1) inPlaceholders.append(", ");
        }

        String processedSql = sqlPart.replace("?", inPlaceholders.toString());
        return this.append(processedSql, args);
    }

    /**
     * 1. SimpleDb에서 현재 스레드 전용 커넥션(Connection)을 얻어옴.
     * 2. 그 커넥션을 이용해 '문자열 쿼리'를 탑재한 PreparedStatement(배달부)를 생성.
     * 3. 쿼리 조립 과정에서 params 리스트에 모아둔 파라미터(args)들을
     * 물음표(?) 자리에 i+1 순서대로 안전하게 매핑(setObject).
     * 4. 쿼리문과 데이터가 결합된, 바로 쏠 수 있는 PreparedStatement를 리턴하는 메서드.
     */
    private PreparedStatement prepare() throws SQLException {
        Connection conn = simpleDb.getConnection();
        PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString(), Statement.RETURN_GENERATED_KEYS);

        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
        return stmt;
    }

    /**
     * ==========================================
     * 중복 제거를 위한 공통 템플릿 메서드
     * ==========================================
     * PreparedStatement > Statement > AutoCloseable, Wrapper
     * ResultSet > AutoCloseable, Wrapper
     * 둘다 AutoCloseable이므로 try-with-resources로 자동 자원 해제 가능
     * try 중괄호를 벗어나자마자 close를 실행하므로, 자원 누수 걱정 없이 안전하게 사용할 수 있음
     */
    private <T> T executeQueryAndMap(ResultSetMapper<T> mapper) {
        try (PreparedStatement stmt = prepare(); ResultSet rs = stmt.executeQuery()) {
            return mapper.map(rs); // 람다식으로 넘겨받은 알맹이 로직(map)에 결과 표(rs)를 던져서 가공 처리함
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ==========================================

    public long insert() {
        try (PreparedStatement stmt = prepare()) {
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        try (PreparedStatement stmt = prepare()) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        return update();
    }

    /**
     * DB에서 여러 개의 행(다건)을 조회하여 리스트 맵 구조로 치환하는 메서드
     * 1. ResultSetMetaData는 받아온 결과 표(rs)의 '사용설명서'다. 컬럼 개수나 이름, 타입을 다 들고 있삼.
     * 2. rs.next()가 true를 반환하는 동안(읽을 행이 남아있는 동안) 엑셀 밑으로 한 줄씩 내리며 반복 수행함.
     * 3. 한 줄 내려갈 때마다 새로운 LinkedHashMap(순서 보장 맵)을 파서 컬럼 이름(Key)과 데이터(Value)를 쑤셔넣음.
     * 4. 데이터 타입이 MySQL용 Timestamp나 Bit이면 자바 표준인 LocalDateTime, Boolean으로 안전하게 형변환해줌.
     * 5. 완성된 한 줄짜리 Map들을 List에 차곡차곡 쌓아서 최종 리턴함.
     */
    public List<Map<String, Object>> selectRows() {
        return executeQueryAndMap(rs -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    if (value instanceof Timestamp) {
                        value = ((Timestamp) value).toLocalDateTime();
                    } else if (value instanceof Boolean || (metaData.getColumnType(i) == Types.BIT)) {
                        value = rs.getBoolean(i);
                    }
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            return rows;
        });
    }

    /**
     * 딱 한 줄(단건)만 조회하는 메서드
     * 위에서 만든 다건 조회(selectRows)를 그대로 재활용하되,
     * 비어있으면 널(null)을 던지고, 데이터가 있으면 첫 번째(0번 인덱스) 엑셀 한 줄 맵만 쏙 빼서 돌려준다.
     */
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * DB 데이터 맵을 특정 클래스(예: Article.class) 인스턴스 리스트로 자동 매핑함
     * 1. 먼저 selectRows()를 돌려 DB 원본 Map 리스트를 확보.
     * 2. 자바 리플렉션 기술(cls.getDeclaredConstructor().newInstance())을 써서 런타임에 해당 클래스의 깡통 객체(obj)를 생성함.
     * 3. DB 표에서 읽어온 컬럼명(entry.getKey())과 정확히 일치하는 자바 클래스의 필드(Field field)를 강제로 찾아냄.
     * 4. private 필드여도 침범할 수 있게 field.setAccessible(true)를 사용하여 뚫어버린 뒤, 아까 생성한 객체에 데이터를 밀어넣음(field.set).
     * 5. DTO에 없는 컬럼명 때문에 NoSuchFieldException 폭탄이 터지면 무시(catch)하고 패스.
     */
    public <T> List<T> selectRows(Class<T> cls) {
        List<Map<String, Object>> rows = selectRows();
        List<T> result = new ArrayList<>();
        try {
            for (Map<String, Object> row : rows) {
                T obj = cls.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    try {
                        Field field = cls.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(obj, entry.getValue());
                    } catch (NoSuchFieldException e) {
                        // DTO 변수명과 매칭되지 않는 컬럼 무시
                    }
                }
                result.add(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 리플렉션 객체 매핑 버전의 단건 조회 메서드
     * 바로 위에서 구현한 selectRows(cls) 결과 목록 중 첫 번째 녀석만 안전하게 낚아채서 반환.
     */
    public <T> T selectRow(Class<T> cls) {
        List<T> list = selectRows(cls);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * SELECT NOW() 같이 단 하나의 날짜/시간(LocalDateTime) 값만 뽑아올 때 쓰는 메서드
     * rs.next()로 첫 줄 눈금쇠를 맞추고, 표의 첫 번째 칸(1번 인덱스)에서 Timestamp를 꺼내 LocalDateTime으로 형변환 후 리턴.
     */
    public LocalDateTime selectDatetime() {
        return executeQueryAndMap(rs -> {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
            return null;
        });
    }

    /**
     * SELECT COUNT(*) 이나 SELECT id 처럼 딱 하나의 숫자형(Long) 결과만 필요할 때 씀.
     * 표의 첫 번째 칸에서 rs.getLong(1)로 숫자를 깔끔하게 떼어온다. 데이터가 없으면 null 반환.
     */
    public Long selectLong() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getLong(1) : null);
    }

    /**
     * 특정 ID 번호 목록들만 List<Long> 형태로 한 번에 싹 긁어오고 싶을 때 씀.
     * rs.next() 반복문을 돌려가며 첫 번째 열에 걸린 숫자들을 순수하게 리스트에 모아서 던져준다.
     */
    public List<Long> selectLongs() {
        return executeQueryAndMap(rs -> {
            List<Long> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getLong(1));
            }
            return list;
        });
    }

    /**
     * SELECT title 처럼 딱 한 칸짜리 글자 데이터(String)가 필요할 때 씀.
     * 문법 메커니즘은 selectLong()과 완전히 똑같고 꺼내오는 바구니 타입(getString)만 문자형이다.
     */
    public String selectString() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * SELECT isBlind 나 SELECT 1 = 1 같이 참/거짓(Boolean) 판별용 결과를 단건으로 꺼낼 때 씀.
     * 표의 첫 번째 칸에서 rs.getBoolean(1)로 참/거짓 신호를 수신해 전달한다.
     */
    public Boolean selectBoolean() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getBoolean(1) : null);
    }

    @Override
    public void close() {
        // 자원 해제용  (미구현함)
    }
}