package io.github.siyukio.postgresql.boot.starter.autoconfigure;

import io.github.siyukio.postgresql.registrar.PostgresqlEntityRegistrar;
import org.springframework.context.annotation.Import;

/**
 * @author Bugee
 */

@Import({PostgresqlEntityRegistrar.class})
public class SiyukioPostgresqlAutoConfiguration {

}
