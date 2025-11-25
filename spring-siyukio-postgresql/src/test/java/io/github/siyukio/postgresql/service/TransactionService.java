package io.github.siyukio.postgresql.service;

import io.github.siyukio.postgresql.entity.RecordEvent;
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
    private PgEntityDao<RecordEvent> recordEventPgEntityDao;

    @Transactional
    public void insertWithTransaction() {
        RecordEvent recordEvent = RecordEvent.builder()
                .id(IdUtils.getUniqueId())
                .content("first insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEvent);

        boolean exist = this.recordEventPgEntityDao.existById(recordEvent.id());
        log.info("insertWithTransaction exist:{},{}", recordEvent.id(), exist);

        recordEvent = RecordEvent.builder()
                .id(IdUtils.getUniqueId())
                .content("second insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEvent);
    }

    @Transactional
    public void insertWithRollback() {
        RecordEvent recordEvent = RecordEvent.builder()
                .id(IdUtils.getUniqueId())
                .content("first insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEvent);

        boolean exist = this.recordEventPgEntityDao.existById(recordEvent.id());
        log.info("insertWithRollback exist:{},{}", recordEvent.id(), exist);

        recordEvent = RecordEvent.builder()
                .id(IdUtils.getUniqueId())
                .content("second insert")
                .type("insert")
                .build();
        this.recordEventPgEntityDao.insert(recordEvent);
        this.recordEventPgEntityDao.insert(recordEvent);
        throw new RuntimeException("rollback");
    }
}
