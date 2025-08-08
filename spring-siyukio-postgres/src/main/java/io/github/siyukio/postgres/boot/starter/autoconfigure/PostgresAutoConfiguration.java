package io.github.siyukio.postgres.boot.starter.autoconfigure;

import io.github.siyukio.postgres.registrar.PostgresEntityRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author Bugee
 */

@AutoConfigureAfter({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@Import({PostgresEntityRegistrar.class})
public class PostgresAutoConfiguration {

}
