package io.github.siyukio.application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SiyukioMcpServerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SiyukioMcpServerApplication.class)
//                .properties("spring.config.additional-location=/opt/configmap/")
                .build()
                .run(args);
    }

}
