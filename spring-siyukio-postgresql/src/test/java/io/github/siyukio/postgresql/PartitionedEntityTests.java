package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.entity.PartitionedEntity;
import io.github.siyukio.tools.entity.EntityConstants;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
@SpringBootTest
public class PartitionedEntityTests {

    @Autowired
    private PgEntityDao<PartitionedEntity> partitionedPgEntityDao;

    @Test
    public void testInsert() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("insert")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);
        log.info("partitionedEntity: {}", partitionedEntity);
    }

    @Test
    public void testInsertBatch() {
        List<PartitionedEntity> partitionedEntities = new ArrayList<>();
        partitionedEntities.add(PartitionedEntity.builder()
                .message("insertBatch")
                .build());
        int num = this.partitionedPgEntityDao.insertBatch(partitionedEntities);
        log.info("{}", num);
    }

    @Test
    public void testQueryWithId() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("queryById")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        partitionedEntity = this.partitionedPgEntityDao.queryById(partitionedEntity.id());
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntity));
    }

    @Test
    public void testQueryAndUpdate() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("insert")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        partitionedEntity = partitionedEntity.withMessage("update");

        partitionedEntity = this.partitionedPgEntityDao.update(partitionedEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntity));
    }

    @Test
    public void testUpdateBatch() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("insert")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        partitionedEntity = partitionedEntity.withMessage("updateBatch");

        int num = this.partitionedPgEntityDao.updateBatch(List.of(partitionedEntity));
        log.info("{}", num);
    }

    @Test
    public void testUpsert() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .id(IdUtils.getUniqueId())
                .message("insert")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);
        partitionedEntity = partitionedEntity.withMessage("upsert");
        partitionedEntity = this.partitionedPgEntityDao.upsert(partitionedEntity);
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntity));
    }

    @Test
    public void testExistById() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("exist")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        boolean exist = this.partitionedPgEntityDao.existById(partitionedEntity.id());
        log.info("{}", exist);
    }

    @Test
    public void testDeleteById() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("delete")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        int num = this.partitionedPgEntityDao.deleteById(partitionedEntity.id());
        log.info("{}", num);
    }

    @Test
    public void testDeleteEntity() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("delete")
                .build();
        partitionedEntity = this.partitionedPgEntityDao.insert(partitionedEntity);

        int num = this.partitionedPgEntityDao.delete(partitionedEntity);
        log.info("{}", num);
    }

    @Test
    public void testDeleteByQuery() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("deleteByQuery")
                .build();
        this.partitionedPgEntityDao.insert(partitionedEntity);

        QueryBuilder queryBuilder = QueryBuilders.termQuery("message", "deleteByQuery");
        int num = this.partitionedPgEntityDao.deleteByQuery(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testCount() {
        int num = this.partitionedPgEntityDao.queryCount();
        log.info("{}", num);
    }

    @Test
    public void testCountByQuery() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("queryCount")
                .build();
        this.partitionedPgEntityDao.insert(partitionedEntity);

        QueryBuilder queryBuilder = QueryBuilders.termQuery("message", "queryCount");
        int num = this.partitionedPgEntityDao.queryCount(queryBuilder);
        log.info("{}", num);
    }

    @Test
    public void testQueryOne() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("queryOne")
                .build();
        this.partitionedPgEntityDao.insert(partitionedEntity);

        QueryBuilder queryBuilder = QueryBuilders.termQuery("message", "queryOne");
        partitionedEntity = this.partitionedPgEntityDao.queryOne(queryBuilder);
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntity));
    }

    @Test
    public void testQueryList() {
        List<PartitionedEntity> partitionedEntities = this.partitionedPgEntityDao.queryList(0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntities));
    }

    @Test
    public void testQueryListAndSort() {
        SortBuilder sortBuilder = SortBuilders.fieldSort(EntityConstants.CREATED_AT_TS_FIELD).order(SortOrder.DESC);
        List<PartitionedEntity> partitionedEntities = this.partitionedPgEntityDao.queryList(sortBuilder, 0, 10);
        log.info("{}", XDataUtils.toPrettyJSONString(partitionedEntities));
    }

    @Test
    public void testPage() {
        PartitionedEntity partitionedEntity = PartitionedEntity.builder()
                .message("queryPage")
                .build();
        this.partitionedPgEntityDao.insert(partitionedEntity);

        QueryBuilder queryBuilder = QueryBuilders.termQuery("message", "queryPage");
        SortBuilder sortBuilder = SortBuilders.fieldSort(EntityConstants.CREATED_AT_TS_FIELD).order(SortOrder.DESC);
        Page<PartitionedEntity> page = this.partitionedPgEntityDao.queryPage(queryBuilder, sortBuilder, 0, 2);
        log.info("{}", XDataUtils.toPrettyJSONString(page));
    }
}
