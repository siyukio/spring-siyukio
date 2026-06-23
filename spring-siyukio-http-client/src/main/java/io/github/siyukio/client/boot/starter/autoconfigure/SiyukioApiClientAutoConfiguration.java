package io.github.siyukio.client.boot.starter.autoconfigure;

import io.github.siyukio.client.registrar.ApiClientRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Bugee
 */
@Import({ApiClientRegistrar.class})
@AutoConfigureAfter({WebMvcAutoConfiguration.class})
@ComponentScan("io.github.siyukio.client")
public class SiyukioApiClientAutoConfiguration {
}
