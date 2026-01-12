package io.github.siyukio.postgresql.service;

import io.github.siyukio.postgresql.entity.RecordEventEntity;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.util.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Bugee
 */
@Slf4j
@Service
public class TransactionService {

    @Autowired
    private PgEntityDao<RecordEventEntity> recordEventPgEntityDao;

    @Transactional
    public void insertWithTransaction() {
        RecordEventEntity recordEventEntity = RecordEventEntity.builder()
                .id(IdUtils.getUniqueId())
                .content("first insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEventEntity);

        boolean exist = this.recordEventPgEntityDao.existById(recordEventEntity.id());
        log.info("insertWithTransaction exist:{},{}", recordEventEntity.id(), exist);

        recordEventEntity = RecordEventEntity.builder()
                .id(IdUtils.getUniqueId())
                .content("second insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEventEntity);
    }

    @Transactional
    public void insertWithRollback() {
        RecordEventEntity recordEventEntity = RecordEventEntity.builder()
                .id(IdUtils.getUniqueId())
                .content("first insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEventEntity);

        boolean exist = this.recordEventPgEntityDao.existById(recordEventEntity.id());
        log.info("insertWithRollback exist:{},{}", recordEventEntity.id(), exist);

        recordEventEntity = RecordEventEntity.builder()
                .id(IdUtils.getUniqueId())
                .content("second insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEventEntity);
        this.recordEventPgEntityDao.insert(recordEventEntity);
        throw new RuntimeException("rollback");
    }
}
