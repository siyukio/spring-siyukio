package io.github.siyukio.tools.registrar;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Bugee
 */
@Slf4j
public abstract class AbstractDataSourceRegistrar extends AbstractCommonRegistrar {

    protected abstract HikariDataSource createDataSource();

    /*
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator generator) {
        super.registerBeanDefinitions(metadata, registry, generator);

        HikariDataSource hikariDataSource = this.createDataSource();
        if (registry.containsBeanDefinition("dataSource")) {
            return;
        }

        BeanDefinition dataSourceBeanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(HikariDataSource.class, () -> hikariDataSource)
                .setPrimary(true)
                .getBeanDefinition();
        registry.registerBeanDefinition("dataSource", dataSourceBeanDefinition);
        log.info("Bootstrapping set default HikariDataSource: {}", dataSourceBeanDefinition);
    }

}
