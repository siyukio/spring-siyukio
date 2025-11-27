package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.entity.RecordEvent;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.query.QueryBuilders;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilders;
import io.github.siyukio.tools.entity.sort.SortOrder;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Bugee
 */
@Slf4j
@SpringBootTest
public class RecordEventTests {

    private final String id = "test";

    @Autowired
    private PgEntityDao<RecordEvent> recordEventPgEntityDao;

    private RecordEvent createRandom() {
        JSONObject metadataJson = new JSONObject();
        metadataJson.put("model", "gpt-5-chat");

        JSONArray messages = new JSONArray();
        JSONObject messageJson = new JSONObject();
        messageJson.put("role", "user");
        messageJson.put("text", "hello");
        messages.put(messageJson);

        RecordEvent.Item item = RecordEvent.Item.builder()
                .costMs(1000)
                .build();

        return RecordEvent.builder()
                .error(false)
                .costMs(1000)
                .content("test")
                .type("user")
                .rating(0.3)
                .metadata(metadataJson)
                .messages(messages)
                .item(item)
                .items(List.of(item))
                .build();
    }

    @Test
    public void testInsert() {
        RecordEvent recordEvent = this.createRandom();
        recordEvent = this.recordEventPgEntityDao.insert(recordEvent);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }

    @Test
    public void testInsertBatch() {
        List<RecordEvent> recordEvents = new ArrayList<>();
        recordEvents.add(this.createRandom());
        int num = this.recordEventPgEntityDao.insertBatch(recordEvents);
        log.info("{}", num);
    }

    @Test
    public void testInsertWithId() {
        RecordEvent recordEvent = this.createRandom();
        recordEvent = recordEvent.withId(this.id);
        recordEvent = this.recordEventPgEntityDao.insert(recordEvent);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }

    @Test
    public void testQueryWithId() {
        RecordEvent recordEvent = this.recordEventPgEntityDao.queryById(this.id);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }

    @Test
    public void testQueryAndUpdate() {
        RecordEvent recordEvent = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEvent == null) {
            throw new RuntimeException("id not found");
        }
        recordEvent = recordEvent.withContent("update")
                .withError(true);
        recordEvent = this.recordEventPgEntityDao.update(recordEvent);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }

    @Test
    public void testUpdateBatch() {
        List<RecordEvent> recordEvents = new ArrayList<>();
        recordEvents.add(this.createRandom());

        recordEvents = recordEvents.stream()
                .map(recordEvent -> recordEvent.withId(IdUtils.getUniqueId()))
                .collect(Collectors.toCollection(ArrayList::new));

        RecordEvent recordEvent = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEvent == null) {
            throw new RuntimeException("id not found");
        }
        recordEvent = recordEvent.withContent("update batch");
        recordEvents.add(recordEvent);
        int num = this.recordEventPgEntityDao.updateBatch(recordEvents);
        log.info("{}", num);
    }

    @Test
    public void testUpsertWithNewId() {
        RecordEvent recordEvent = this.createRandom().withId(IdUtils.getUniqueId()).withType("upsert new id");
        recordEvent = this.recordEventPgEntityDao.upsert(recordEvent);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }

    @Test
    public void testUpsertWithExistId() {
        RecordEvent recordEvent = this.createRandom().withId(this.id).withType("upsert exist id");
        recordEvent = this.recordEventPgEntityDao.upsert(recordEvent);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvent));
    }


    @Test
    public void testDeleteById() {
        int num = this.recordEventPgEntityDao.deleteById(this.id);
        log.info("{}", num);
    }

    @Test
    public void testDeleteEntity() {
        RecordEvent recordEvent = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEvent == null) {
            throw new RuntimeException("id not found");
        }
        int num = this.recordEventPgEntityDao.delete(recordEvent);
        log.info("{}", num);
    }

    @Test
    public void testDeleteByQuery() {
        QueryBuilder queryBuilder = QueryBuilders.termQuery("type", "user");
        int num = this.recordEventPgEntityDao.deleteByQuery(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testCount() {
        int num = this.recordEventPgEntityDao.count();
        log.info("{}", num);
    }

    @Test
    public void testCountByQuery() {
        LocalDateTime maxCreatedAt = XDataUtils.parse("2025-09-08 11:35:35");
        long maxCreateAtTs = XDataUtils.toMills(maxCreatedAt);
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createdAtTs").lt(maxCreateAtTs);
        int num = this.recordEventPgEntityDao.countByQuery(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testQuery() {
        List<RecordEvent> recordEvents = this.recordEventPgEntityDao.query(0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvents));
    }

    @Test
    public void testQuery2() {
        LocalDateTime maxCreatedAt = XDataUtils.parse("2025-09-08 11:35:35");
        long maxCreateAtTs = XDataUtils.toMills(maxCreatedAt);
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createdAtTs").lt(maxCreateAtTs);
        List<RecordEvent> recordEvents = this.recordEventPgEntityDao.query(queryBuilder, 0, 1);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvents));
    }

    @Test
    public void testQuery3() {
        LocalDateTime maxCreatedAt = XDataUtils.parse("2025-09-08 11:35:36");
        long maxCreateAtTs = XDataUtils.toMills(maxCreatedAt);
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createdAtTs").lte(maxCreateAtTs);

        SortBuilder sortBuilder = SortBuilders.fieldSort("createdAtTs").order(SortOrder.DESC);
        List<RecordEvent> recordEvents = this.recordEventPgEntityDao.query(queryBuilder, sortBuilder, 0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEvents));
    }

    @Test
    public void testQueryPage() {
        Date maxDate = new Date();
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createTime").lte(maxDate.getTime());

        SortBuilder sortBuilder = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);
        Page<RecordEvent> page = this.recordEventPgEntityDao.queryPage(queryBuilder, sortBuilder, 1, 1);
        log.info("{}", XDataUtils.toPrettyJSONString(page));
    }
}
