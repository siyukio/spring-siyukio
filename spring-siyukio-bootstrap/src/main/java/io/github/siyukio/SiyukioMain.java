package io.github.siyukio;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Bugee
 */

@SpringBootApplication
public class SiyukioMain {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SiyukioMain.class)
                .build()
                .run(args);
    }
}
