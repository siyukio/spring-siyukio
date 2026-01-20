package io.github.siyukio.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Bugee
 */

@SpringBootApplication
public class CommonSpringBootMain {

    public static void main(String[] args) {
        new SpringApplicationBuilder(CommonSpringBootMain.class)
                .build()
                .run(args);
    }
}
