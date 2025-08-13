package io.github.siyukio.postgresql.boot.starter.autoconfigure;

import io.github.siyukio.postgresql.registrar.PostgresqlEntityRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author Bugee
 */

@AutoConfigureAfter({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@Import({PostgresqlEntityRegistrar.class})
public class PostgresqlAutoConfiguration {

}
