package io.github.siyukio.postgresql.service;

import io.github.siyukio.postgresql.entity.TestEvent;
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
    private PgEntityDao<TestEvent> testEventPgEntityDao;

    @Transactional
    public void insertWithTransaction() {
        TestEvent testEvent = new TestEvent();
        testEvent.id = IdUtils.getUniqueId();
        testEvent.content = "first insert";
        testEvent.type = "insert";
        this.testEventPgEntityDao.insert(testEvent);

        boolean exist = this.testEventPgEntityDao.existById(testEvent.id);
        log.info("insertWithTransaction exist:{},{}", testEvent.id, exist);

        testEvent = new TestEvent();
        testEvent.id = IdUtils.getUniqueId();
        testEvent.content = "second insert";
        testEvent.type = "insert";
        this.testEventPgEntityDao.insert(testEvent);
    }

    @Transactional
    public void insertWithRollback() {
        TestEvent testEvent = new TestEvent();
        testEvent.id = IdUtils.getUniqueId();
        testEvent.content = "first insert";
        testEvent.type = "insert";
        this.testEventPgEntityDao.insert(testEvent);

        boolean exist = this.testEventPgEntityDao.existById(testEvent.id);
        log.info("insertWithRollback exist:{},{}", testEvent.id, exist);

        testEvent = new TestEvent();
        testEvent.id = IdUtils.getUniqueId();
        testEvent.content = "second insert";
        testEvent.type = "insert";
        this.testEventPgEntityDao.insert(testEvent);
        throw new RuntimeException("rollback");
    }
}
