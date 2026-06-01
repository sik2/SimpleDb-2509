package com.back.simpleDb;

import com.back.Article;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleDbTest {
    private static SimpleDb simpleDb;

    @BeforeAll
    public static void beforeAll() {
        simpleDb = new SimpleDb("localhost", "root", "root123414", "simpleDb__test");
        simpleDb.setDevMode(true);

        createArticleTable();
    }

    @BeforeEach
    public void beforeEach() {
        truncateArticleTable();
        makeArticleTestData();
    }

    private static void createArticleTable() {
        simpleDb.run("DROP TABLE IF EXISTS article");

        simpleDb.run("""
                CREATE TABLE article (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id),
                    createdDate DATETIME NOT NULL,
                    modifiedDate DATETIME NOT NULL,
                    title VARCHAR(100) NOT NULL,
                    `body` TEXT NOT NULL,
                    isBlind BIT(1) NOT NULL DEFAULT 0
                )
                """);
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, 6).forEach(no -> {
            boolean isBlind = no > 3;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            simpleDb.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        simpleDb.run("TRUNCATE article");
    }

    @Test
    @DisplayName("insert")
    public void t001() {
        Sql sql = simpleDb.genSql();
                sql.append("INSERT INTO article")
                .append("SET createdDate = NOW()")
                .append(", modifiedDate = NOW()")
                .append(", title = ?", "제목 new")
                .append(", body = ?", "내용 new");

        long newId = sql.insert();

        assertThat(newId).isGreaterThan(0);
    }

    @Test
    @DisplayName("update")
    public void t002() {
        Sql sql = simpleDb.genSql();

                sql.append("UPDATE article")
                .append("SET title = ?", "제목 new")
                .append("WHERE id IN (?, ?, ?, ?)", 0, 1, 2, 3);
        int affectedRowsCount = sql.update();

        assertThat(affectedRowsCount).isEqualTo(3);
    }

    @Test
    @DisplayName("delete")
    public void t003() {
        Sql sql = simpleDb.genSql();
                sql.append("DELETE")
                .append("FROM article")
                .append("WHERE id IN (?, ?, ?)", 0, 1, 3);
        int affectedRowsCount = sql.delete();

        assertThat(affectedRowsCount).isEqualTo(2);
    }

    @Test
    @DisplayName("selectRows")
    public void t004() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT * FROM article ORDER BY id ASC LIMIT 3");
        List<Map<String, Object>> articleRows = sql.selectRows();

        IntStream.range(0, articleRows.size()).forEach(i -> {
            long id = i + 1;

            Map<String, Object> articleRow = articleRows.get(i);

            assertThat(articleRow.get("id")).isEqualTo(id);
            assertThat(articleRow.get("title")).isEqualTo("제목%d".formatted(id));
            assertThat(articleRow.get("body")).isEqualTo("내용%d".formatted(id));
            assertThat(articleRow.get("createdDate")).isInstanceOf(LocalDateTime.class);
            assertThat(articleRow.get("createdDate")).isNotNull();
            assertThat(articleRow.get("modifiedDate")).isInstanceOf(LocalDateTime.class);
            assertThat(articleRow.get("modifiedDate")).isNotNull();
            assertThat(articleRow.get("isBlind")).isEqualTo(false);
        });
    }

    @Test
    @DisplayName("selectRow")
    public void t005() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT * FROM article WHERE id = 1");
        Map<String, Object> articleRow = sql.selectRow();

        assertThat(articleRow.get("id")).isEqualTo(1L);
        assertThat(articleRow.get("title")).isEqualTo("제목1");
        assertThat(articleRow.get("body")).isEqualTo("내용1");
        assertThat(articleRow.get("createdDate")).isInstanceOf(LocalDateTime.class);
        assertThat(articleRow.get("createdDate")).isNotNull();
        assertThat(articleRow.get("modifiedDate")).isInstanceOf(LocalDateTime.class);
        assertThat(articleRow.get("modifiedDate")).isNotNull();
        assertThat(articleRow.get("isBlind")).isEqualTo(false);
    }

    @Test
    @DisplayName("selectDatetime")
    public void t006() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT NOW()");

        LocalDateTime datetime = sql.selectDatetime();

        long diff = ChronoUnit.SECONDS.between(datetime, LocalDateTime.now());

        assertThat(diff).isLessThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("selectLong")
    public void t007() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT id")
                .append("FROM article")
                .append("WHERE id = 1");

        Long id = sql.selectLong();

        assertThat(id).isEqualTo(1);
    }

    @Test
    @DisplayName("selectString")
    public void t008() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT title")
                .append("FROM article")
                .append("WHERE id = 1");

        String title = sql.selectString();

        assertThat(title).isEqualTo("제목1");
    }

    @Test
    @DisplayName("selectBoolean")
    public void t009() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT isBlind")
                .append("FROM article")
                .append("WHERE id = 1");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(false);
    }

    @Test
    @DisplayName("selectBoolean, 2nd")
    public void t010() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT 1 = 1");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(true);
    }

    @Test
    @DisplayName("selectBoolean, 3rd")
    public void t011() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT 1 = 0");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(false);
    }

    @Test
    @DisplayName("select, LIKE 사용법")
    public void t012() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT COUNT(*)")
                .append("FROM article")
                .append("WHERE id BETWEEN ? AND ?", 1, 3)
                .append("AND title LIKE CONCAT('%', ? '%')", "제목");

        long count = sql.selectLong();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("appendIn")
    public void t013() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT COUNT(*)")
                .append("FROM article")
                .appendIn("WHERE id IN (?)", 1, 2, 3);

        long count = sql.selectLong();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("selectLongs, ORDER BY FIELD 사용법")
    public void t014() {
        Long[] ids = new Long[]{2L, 1L, 3L};

        Sql sql = simpleDb.genSql();
                sql.append("SELECT id")
                .append("FROM article")
                .appendIn("WHERE id IN (?)", ids)
                .appendIn("ORDER BY FIELD (id, ?)", ids);

        List<Long> foundIds = sql.selectLongs();

        assertThat(foundIds).isEqualTo(Arrays.stream(ids).toList());
    }

    @Test
    @DisplayName("selectRows, Article")
    public void t015() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT * FROM article ORDER BY id ASC LIMIT 3");
        List<Article> articleRows = sql.selectRows(Article.class);

        IntStream.range(0, articleRows.size()).forEach(i -> {
            long id = i + 1;

            Article article = articleRows.get(i);

            assertThat(article.getId()).isEqualTo(id);
            assertThat(article.getTitle()).isEqualTo("제목%d".formatted(id));
            assertThat(article.getBody()).isEqualTo("내용%d".formatted(id));
            assertThat(article.getCreatedDate()).isInstanceOf(LocalDateTime.class);
            assertThat(article.getCreatedDate()).isNotNull();
            assertThat(article.getModifiedDate()).isInstanceOf(LocalDateTime.class);
            assertThat(article.getModifiedDate()).isNotNull();
            assertThat(article.isBlind()).isEqualTo(false);
        });
    }

    @Test
    @DisplayName("selectRow, Article")
    public void t016() {
        Sql sql = simpleDb.genSql();
                sql.append("SELECT * FROM article WHERE id = 1");
        Article article = sql.selectRow(Article.class);

        Long id = 1L;

        assertThat(article.getId()).isEqualTo(id);
        assertThat(article.getTitle()).isEqualTo("제목%d".formatted(id));
        assertThat(article.getBody()).isEqualTo("내용%d".formatted(id));
        assertThat(article.getCreatedDate()).isInstanceOf(LocalDateTime.class);
        assertThat(article.getCreatedDate()).isNotNull();
        assertThat(article.getModifiedDate()).isInstanceOf(LocalDateTime.class);
        assertThat(article.getModifiedDate()).isNotNull();
        assertThat(article.isBlind()).isEqualTo(false);
    }
    @Test
    @DisplayName("use in multi threading")
    public void t017() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCounter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        Runnable task = () -> {
            try {
                Sql sql = simpleDb.genSql();
                sql.append("SELECT * FROM article WHERE id = 1");
                Article article = sql.selectRow(Article.class);
                Long id = 1L;
                if (article.getId() == id &&
                        article.getTitle().equals("제목%d".formatted(id)) &&
                        article.getBody().equals("내용%d".formatted(id)) &&
                        article.getCreatedDate() != null &&
                        article.getModifiedDate() != null &&
                        !article.isBlind()) {
                    successCounter.incrementAndGet();
                }
            } finally {
                simpleDb.close();
                latch.countDown();
            }
        };
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task);
        }
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        assertThat(successCounter.get()).isEqualTo(numberOfThreads);
    }

    @Test
    @DisplayName("rollback")
    public void t018() {
        long oldCount = simpleDb.genSql()
                .append("SELECT COUNT(*)")
                .append("FROM article")
                .selectLong();
        simpleDb.startTransaction();

        simpleDb.genSql()
                .append("INSERT INTO article ")
                .append("(createdDate, modifiedDate, title, body)")
                .appendIn("VALUES (NOW(), NOW(), ?)", "새 제목", "새 내용")
                .insert();

        simpleDb.rollback();

        long newCount = simpleDb.genSql()
                .append("SELECT COUNT(*)")
                .append("FROM article")
                .selectLong();

        assertThat(newCount).isEqualTo(oldCount);
    }

    @Test
    @DisplayName("commit")
    public void t019() {
        long oldCount = simpleDb.genSql()
                .append("SELECT COUNT(*)")
                .append("FROM article")
                .selectLong();
        simpleDb.startTransaction();

        simpleDb.genSql()
                .append("INSERT INTO article ")
                .append("(createdDate, modifiedDate, title, body)")
                .appendIn("VALUES (NOW(), NOW(), ?)", "새 제목", "새 내용")
                .insert();

        simpleDb.commit();

        long newCount = simpleDb.genSql()
                .append("SELECT COUNT(*)")
                .append("FROM article")
                .selectLong();

        assertThat(newCount).isEqualTo(oldCount + 1);
    }
}
