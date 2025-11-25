package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Bugee
 */
@Slf4j
@SpringBootTest
public class TransactionTests {

    private final String id = "test";

    @Autowired
    private TransactionService transactionService;

    @Test
    public void testTransaction() {
        this.transactionService.insertWithTransaction();
    }

    @Test
    public void testRollback() {
        this.transactionService.insertWithRollback();
    }

}
