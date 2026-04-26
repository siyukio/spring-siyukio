package io.github.siyukio.tools.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;

/**
 * @author Bugee
 */
@Slf4j
public abstract class AbstractDataSourceRegistrar extends AbstractCommonRegistrar {

    protected abstract DataSource createDataSource(BeanDefinitionRegistry registry);

    /*
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator generator) {
        super.registerBeanDefinitions(metadata, registry, generator);

        DataSource dataSource = this.createDataSource(registry);
        if (registry.containsBeanDefinition("dataSource")) {
            return;
        }

        BeanDefinition dataSourceBeanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(DataSource.class, () -> dataSource)
                .setPrimary(true)
                .getBeanDefinition();
        registry.registerBeanDefinition("dataSource", dataSourceBeanDefinition);
        log.info("Bootstrapping set default HikariDataSource: {}", dataSourceBeanDefinition);
    }

}
