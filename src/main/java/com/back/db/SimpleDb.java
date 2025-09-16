package com.back.db;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class SimpleDb {

    private final String host;
    private final String userName;
    private final String password;
    private final String database;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private static final int PORT = 3306;

    public SimpleDb(String host, String userName, String password, String database) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.database = database;
    }

    // TODO : 커넥션 풀을 구현하여 thread-safe 커넥션 획득
    private Connection getConnection() throws SQLException {
        String URL = "jdbc:mysql://" + host + ":" + PORT + "/" + database;
        Connection connection = connectionHolder.get();
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, userName, password);
            connectionHolder.set(connection);
        }
        return connection;
    }

    // throws SQLException을 명시하기 위한 Function<PreparedStatement, T> 함수형 인터페이스
    // 바인딩 완료된 PreparedStatement 이용해, 쿼리를 실행하고 적절한 결과값을 반환하도록 구현한다
    @FunctionalInterface
    interface StatementCallback<T> {
        T apply(PreparedStatement statement) throws SQLException;
    }

    private <T> T runTemplate(String sql, Object[] args, StatementCallback<T> callback) {
        // devMode일 경우, trace 레벨도 출력
        log.info("Executing SQL: {}", sql.trim());
        log.trace("Args {}", Arrays.toString(args));
        log.info("=======================================");

        // try - with resources (JAVA 7 이상)
        Connection connection = null;
        try {
            connection = getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                // 바인딩할 값이 없을 경우 생략
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        statement.setObject(i + 1, args[i]);
                    }
                }

                return callback.apply(statement);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null && connection.getAutoCommit()) {
                    connection.close();
                    connectionHolder.remove();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection connection = connectionHolder.get();
            connection.commit();
            close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection connection = connectionHolder.get();
            connection.rollback();
            close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            Connection connection = connectionHolder.get();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionHolder.remove();
        }
    }

    // 바인딩할 값이 없는 경우
    private <T> T runTemplate(String sql, SimpleDb.StatementCallback<T> callback) {
        return runTemplate(sql, null, callback);
    }

    public void run(String sql, Object... args) {
        runTemplate(sql, args, PreparedStatement::executeUpdate);
    }

    public void run(String sql) {
        runTemplate(sql, PreparedStatement::executeUpdate);
    }

    private long runInsert(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        });
    }

    private int runUpdate(String sql, Object... args) {
        return runTemplate(sql, args, PreparedStatement::executeUpdate);
    }

    private int runDelete(String sql, Object... args) {
        return runTemplate(sql, args, PreparedStatement::executeUpdate);
    }

    private Map<String, Object> queryRowToMap(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return convertRowToMap(rs);
            }
            return new HashMap<>();
        });
    }

    private Map<String, Object> convertRowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            row.put(metaData.getColumnName(i), rs.getObject(i));
        }
        return row;
    }

    private List<Map<String, Object>> queryRowsToMaps(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(convertRowToMap(rs));
                }
                return rows;
            }
        });
    }

    private <T> T queryRow(String sql, Object[] args, Class<T> clazz) {
        return runTemplate(sql, args, statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                if (rs.next()) {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= columnCount; i++) {
                        // TODO : Reflection 데이터 캐싱
                        bindArguments(clazz, rs, metaData, i, instance);
                    }
                    return instance;
                }
                return null;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private <T> List<T> queryRows(String sql, Object[] args, Class<T> clazz) {
        return runTemplate(sql, args, statement -> {
            List<T> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (int i = 1; i <= columnCount; i++) {
                        // TODO : Reflection 데이터 캐싱
                        bindArguments(clazz, rs, metaData, i, instance);
                    }
                    rows.add(instance);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            return rows;
        });
    }

    private <T> void bindArguments(Class<T> clazz, ResultSet rs, ResultSetMetaData metaData, int i, T instance)
            throws SQLException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String columnName = metaData.getColumnName(i);

        String fieldName = convertCamelCase(columnName);
        Field field = clazz.getDeclaredField(fieldName);

        String setterName = convertSetterName(fieldName, field.getType());
        Method setter = clazz.getMethod(setterName, field.getType());

        setter.invoke(instance, rs.getObject(i));
    }

    private String convertCamelCase(String columnName) {
        while (columnName.endsWith("_")) {
            columnName = columnName.substring(0, columnName.length() - 1);
        }
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : columnName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String convertSetterName(String columnName, Class<?> fieldType) {
        if (fieldType.equals(boolean.class) && columnName.startsWith("is")) {
            return "set" + columnName.substring(2, 3).toUpperCase() + columnName.substring(3);
        }
        return "set" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
    }

    private <T> T queryColumn(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return (T) rs.getObject(1);
                }
                return null;
            }
        });
    }

    private <T> List<T> queryColumns(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                List<T> columns = new ArrayList<>();
                while (rs.next()) {
                    columns.add((T) rs.getObject(1));
                }
                return columns;
            }
        });
    }

    private Boolean queryBooleanColumn(String sql, Object... args) {
        return runTemplate(sql, args, statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
                return null;
            }
        });
    }

    // devMode true 설정시 log레벨 변경
    public void setDevMode(boolean devMode) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(SimpleDb.class);
        if (devMode) {
            logger.setLevel(Level.TRACE);
        } else {
            // default
            logger.setLevel(Level.INFO);
        }
    }

    public Sql genSql() {
        return new Sql();
    }

    public class Sql {

        private final StringBuilder builder;
        private final List<Object> bindingArgs;

        public Sql() {
            builder = new StringBuilder();
            bindingArgs = new ArrayList<>();
        }

        public Sql append(String sql, Object... args) {
            builder.append(sql).append(" ");
            bindingArgs.addAll(Arrays.asList(args));
            return this;
        }

        // 가변인자에 아무 것도 전달되지 않아도 String sql만 전달 된다면 append는 컴파일 에러 발생하지않고 잘 작동함
        // 하지만, 이 메소드가 따로 선언 되는 것이 더 나은 사용자 경험이라고 생각하여 추가하였음
        // 이는 SimpleDb.run()도 마찬가지임
        public Sql append(String sql) {
            builder.append(sql).append(" ");
            return this;
        }

        public Sql appendIn(String sql, Object... args) {
            StringJoiner joiner = new StringJoiner(", ");
            for (int i = 0; i < args.length; i++) {
                joiner.add("?");
            }
            String replace = sql.replace("?", joiner.toString());
            builder.append(replace).append(" ");

            bindingArgs.addAll(Arrays.asList(args));

            return this;
        }

        public long insert() {
            return SimpleDb.this.runInsert(builder.toString(), bindingArgs.toArray());
        }

        public int update() {
            return SimpleDb.this.runUpdate(builder.toString(), bindingArgs.toArray());
        }

        public int delete() {
            return SimpleDb.this.runDelete(builder.toString(), bindingArgs.toArray());
        }

        public Map<String, Object> selectRow() {
            return SimpleDb.this.queryRowToMap(builder.toString(), bindingArgs.toArray());
        }

        public <T> T selectRow(Class<T> clazz) {
            return queryRow(builder.toString(), bindingArgs.toArray(), clazz);
        }

        public List<Map<String, Object>> selectRows() {
            return SimpleDb.this.queryRowsToMaps(builder.toString(), bindingArgs.toArray());
        }

        public <T> List<T> selectRows(Class<T> clazz) {
            return queryRows(builder.toString(), bindingArgs.toArray(), clazz);
        }

        public LocalDateTime selectDatetime() {
            return SimpleDb.this.queryColumn(builder.toString(), bindingArgs.toArray());
        }

        public Long selectLong() {
            return SimpleDb.this.queryColumn(builder.toString(), bindingArgs.toArray());
        }

        public String selectString() {
            return SimpleDb.this.queryColumn(builder.toString(), bindingArgs.toArray());
        }

        public List<Long> selectLongs() {
            return SimpleDb.this.queryColumns(builder.toString(), bindingArgs.toArray());
        }

        public Boolean selectBoolean() {
            return SimpleDb.this.queryBooleanColumn(builder.toString(), bindingArgs.toArray());
        }
    }
}
