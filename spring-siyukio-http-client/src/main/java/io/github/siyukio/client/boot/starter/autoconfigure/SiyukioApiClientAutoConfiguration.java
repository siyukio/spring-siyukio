package io.github.siyukio.client.boot.starter.autoconfigure;

import io.github.siyukio.client.registrar.ApiClientRegistrar;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Bugee
 */
@Import({ApiClientRegistrar.class})
public class SiyukioApiClientAutoConfiguration {
}
