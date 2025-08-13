package io.github.siyukio.postgresql;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Bugee
 */
@SpringBootApplication
public class PostgresqlApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PostgresqlApplication.class)
//                .properties("spring.config.additional-location=/opt/configmap/")
                .build()
                .run(args);
    }
}
