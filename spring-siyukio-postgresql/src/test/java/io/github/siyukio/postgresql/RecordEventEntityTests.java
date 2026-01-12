package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.entity.RecordEventEntity;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.BoolQueryBuilder;
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
public class RecordEventEntityTests {

    private final String id = "test";

    @Autowired
    private PgEntityDao<RecordEventEntity> recordEventPgEntityDao;

    private RecordEventEntity createRandom() {
        JSONObject metadataJson = new JSONObject();
        metadataJson.put("model", "gpt-5-chat");

        JSONArray messages = new JSONArray();
        JSONObject messageJson = new JSONObject();
        messageJson.put("role", "user");
        messageJson.put("text", "hello");
        messages.put(messageJson);

        RecordEventEntity.Item item = RecordEventEntity.Item.builder()
                .costMs(1000)
                .build();

        return RecordEventEntity.builder()
                .error(false)
                .costMs(1000)
                .content("test")
                .type("user")
                .rating(0.3)
                .metadata(metadataJson)
                .messages(messages)
                .item(item)
                .items(List.of(item))
                .loginType(RecordEventEntity.LoginType.USERNAME)
                .teamId(IdUtils.getUniqueId())
                .build();
    }

    @Test
    public void testInsert() {
        RecordEventEntity recordEventEntity = this.createRandom();
        recordEventEntity = this.recordEventPgEntityDao.insert(recordEventEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }

    @Test
    public void testInsertBatch() {
        List<RecordEventEntity> recordEventEntities = new ArrayList<>();
        recordEventEntities.add(this.createRandom());
        int num = this.recordEventPgEntityDao.insertBatch(recordEventEntities);
        log.info("{}", num);
    }

    @Test
    public void testInsertWithId() {
        RecordEventEntity recordEventEntity = this.createRandom();
        recordEventEntity = recordEventEntity.withId(this.id);
        recordEventEntity = this.recordEventPgEntityDao.insert(recordEventEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }

    @Test
    public void testQueryWithId() {
        RecordEventEntity recordEventEntity = this.recordEventPgEntityDao.queryById(this.id);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }

    @Test
    public void testQueryAndUpdate() {
        RecordEventEntity recordEventEntity = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEventEntity == null) {
            throw new RuntimeException("id not found");
        }
        recordEventEntity = recordEventEntity.withContent("update")
                .withError(true);
        recordEventEntity = this.recordEventPgEntityDao.update(recordEventEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }

    @Test
    public void testUpdateBatch() {
        List<RecordEventEntity> recordEventEntities = new ArrayList<>();
        recordEventEntities.add(this.createRandom());

        recordEventEntities = recordEventEntities.stream()
                .map(recordEventEntity -> recordEventEntity.withId(IdUtils.getUniqueId()))
                .collect(Collectors.toCollection(ArrayList::new));

        RecordEventEntity recordEventEntity = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEventEntity == null) {
            throw new RuntimeException("id not found");
        }
        recordEventEntity = recordEventEntity.withContent("update batch");
        recordEventEntities.add(recordEventEntity);
        int num = this.recordEventPgEntityDao.updateBatch(recordEventEntities);
        log.info("{}", num);
    }

    @Test
    public void testUpsertWithNewId() {
        RecordEventEntity recordEventEntity = this.createRandom().withId(IdUtils.getUniqueId()).withType("upsert new id");
        recordEventEntity = this.recordEventPgEntityDao.upsert(recordEventEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }

    @Test
    public void testUpsertWithExistId() {
        RecordEventEntity recordEventEntity = this.createRandom().withId(this.id).withType("upsert exist id");
        recordEventEntity = this.recordEventPgEntityDao.upsert(recordEventEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntity));
    }


    @Test
    public void testDeleteById() {
        int num = this.recordEventPgEntityDao.deleteById(this.id);
        log.info("{}", num);
    }

    @Test
    public void testDeleteEntity() {
        RecordEventEntity recordEventEntity = this.recordEventPgEntityDao.queryById(this.id);
        if (recordEventEntity == null) {
            throw new RuntimeException("id not found");
        }
        int num = this.recordEventPgEntityDao.delete(recordEventEntity);
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
        List<RecordEventEntity> recordEventEntities = this.recordEventPgEntityDao.query(0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntities));
    }

    @Test
    public void testQuery2() {
        LocalDateTime maxCreatedAt = XDataUtils.parse("2025-09-08 11:35:35");
        long maxCreateAtTs = XDataUtils.toMills(maxCreatedAt);
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createdAtTs").lt(maxCreateAtTs);
        List<RecordEventEntity> recordEventEntities = this.recordEventPgEntityDao.query(queryBuilder, 0, 1);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntities));
    }

    @Test
    public void testQuery3() {
        LocalDateTime maxCreatedAt = XDataUtils.parse("2025-09-08 11:35:36");
        long maxCreateAtTs = XDataUtils.toMills(maxCreatedAt);
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("createdAtTs").lte(maxCreateAtTs);

        SortBuilder sortBuilder = SortBuilders.fieldSort("createdAtTs").order(SortOrder.DESC);
        List<RecordEventEntity> recordEventEntities = this.recordEventPgEntityDao.query(queryBuilder, sortBuilder, 0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(recordEventEntities));
    }

    @Test
    public void testQueryPage() {
        Date maxDate = new Date();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.rangeQuery("createdAtTs").lte(maxDate.getTime()));
        boolQueryBuilder.must(QueryBuilders.termQuery("teamId", "aH8Hr9eALDDxJMX1thF5J"));

        SortBuilder sortBuilder = SortBuilders.fieldSort("createdAtTs").order(SortOrder.DESC);
        Page<RecordEventEntity> page = this.recordEventPgEntityDao.queryPage(boolQueryBuilder, sortBuilder, 1, 1);
        log.info("{}", XDataUtils.toPrettyJSONString(page));
    }
}
