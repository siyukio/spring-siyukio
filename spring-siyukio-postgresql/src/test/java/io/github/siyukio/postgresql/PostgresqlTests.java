package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.entity.TestEvent;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.query.QueryBuilders;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilders;
import io.github.siyukio.tools.entity.sort.SortOrder;
import io.github.siyukio.tools.util.DateUtils;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
@SpringBootTest
public class PostgresqlTests {

    private final String id = "test";
    @Autowired
    private PgEntityDao<TestEvent> testEventPgEntityDao;

    private TestEvent createRandom() {
        JSONObject metadataJson = new JSONObject();
        metadataJson.put("model", "gpt-5-chat");

        JSONArray messages = new JSONArray();
        JSONObject messageJson = new JSONObject();
        messageJson.put("role", "user");
        messageJson.put("text", "hello");
        messages.put(messageJson);

        return TestEvent.builder()
                .error(false)
                .times(1000)
                .content("test")
                .type("user")
                .rating(0.3)
                .metadata(metadataJson)
                .messages(messages)
                .build();
    }

    @Test
    public void testInsert() {
        TestEvent testEvent = this.createRandom();
        testEvent = this.testEventPgEntityDao.insert(testEvent);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvent));
    }

    @Test
    public void testInsertBatch() {
        List<TestEvent> testEvents = new ArrayList<>();
        testEvents.add(this.createRandom());
        int num = this.testEventPgEntityDao.insertBatch(testEvents);
        log.info("{}", num);
    }

    @Test
    public void testInsertWithId() {
        TestEvent testEvent = this.createRandom();
        testEvent.id = this.id;
        testEvent = this.testEventPgEntityDao.insert(testEvent);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvent));
    }

    @Test
    public void testQueryAndUpdate() {
        TestEvent testEvent = this.testEventPgEntityDao.queryById(this.id);
        if (testEvent == null) {
            throw new RuntimeException("id not found");
        }
        testEvent.content = "update";
        testEvent.error = true;
        testEvent = this.testEventPgEntityDao.update(testEvent);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvent));
    }

    @Test
    public void testUpdateBatch() {
        List<TestEvent> testEvents = new ArrayList<>();
        testEvents.add(this.createRandom());
        for (TestEvent testEvent : testEvents) {
            testEvent.id = IdUtils.getUniqueId();
        }

        TestEvent testEvent = this.testEventPgEntityDao.queryById(this.id);
        if (testEvent == null) {
            throw new RuntimeException("id not found");
        }
        testEvent.content = "update batch";
        testEvents.add(testEvent);
        int num = this.testEventPgEntityDao.updateBatch(testEvents);
        log.info("{}", num);
    }

    @Test
    public void testDeleteById() {
        int num = this.testEventPgEntityDao.deleteById(this.id);
        log.info("{}", num);
    }

    @Test
    public void testDeleteEntity() {
        TestEvent testEvent = this.testEventPgEntityDao.queryById(this.id);
        if (testEvent == null) {
            throw new RuntimeException("id not found");
        }
        int num = this.testEventPgEntityDao.delete(testEvent);
        log.info("{}", num);
    }

    @Test
    public void testDeleteByQuery() {
        QueryBuilder queryBuilder = QueryBuilders.termQuery("type", "user");
        int num = this.testEventPgEntityDao.deleteByQuery(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testCount() {
        int num = this.testEventPgEntityDao.count();
        log.info("{}", num);
    }

    @Test
    public void testCountByQuery() {
        Date maxDate = DateUtils.parse("2025-09-08 11:35:35");
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createTime").lt(maxDate.getTime());
        int num = this.testEventPgEntityDao.countByQuery(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testQuery() {
        List<TestEvent> testEvents = this.testEventPgEntityDao.query(0, 10);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvents));
    }

    @Test
    public void testQuery2() {
        Date maxDate = DateUtils.parse("2025-09-08 11:35:35");
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createTime").lt(maxDate.getTime());
        List<TestEvent> testEvents = this.testEventPgEntityDao.query(queryBuilder, 0, 1);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvents));
    }

    @Test
    public void testQuery3() {
        Date maxDate = DateUtils.parse("2025-09-08 11:35:36");
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createTime").lte(maxDate.getTime());

        SortBuilder sortBuilder = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);
        List<TestEvent> testEvents = this.testEventPgEntityDao.query(queryBuilder, sortBuilder, 0, 10);
        log.info("{}", JsonUtils.toPrettyJSONString(testEvents));
    }

    @Test
    public void testQueryPage() {
        Date maxDate = DateUtils.parse("2025-09-08 11:35:36");
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createTime").lte(maxDate.getTime());

        SortBuilder sortBuilder = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);
        Page<TestEvent> page = this.testEventPgEntityDao.queryPage(queryBuilder, sortBuilder, 1, 1);
        log.info("{}", JsonUtils.toPrettyJSONString(page));
    }

}
