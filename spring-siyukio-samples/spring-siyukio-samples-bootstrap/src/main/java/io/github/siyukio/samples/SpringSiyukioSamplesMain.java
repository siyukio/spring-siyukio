package io.github.siyukio.samples;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SpringSiyukioSamplesMain {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringSiyukioSamplesMain.class)
                .build()
                .run(args);
    }
}
