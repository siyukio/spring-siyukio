package io.github.siyukio.postgresql;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SiyukioPgApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SiyukioPgApplication.class)
//                .properties("spring.config.additional-location=/opt/configmap/")
                .build()
                .run(args);
    }

}
