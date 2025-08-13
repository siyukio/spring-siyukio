package io.github.siyukio.postgresql;

import io.github.siyukio.postgresql.entity.TestEvent;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Bugee
 */
@Slf4j
@SpringBootTest
public class PostgresqlTests {

    private PgEntityDao<TestEvent> testEventPgEntityDao;

    @Test
    public void test() {
        log.info("{}", this.testEventPgEntityDao);
    }
}
