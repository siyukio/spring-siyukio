package io.github.siyukio.postgres.registrar;

import io.github.siyukio.postgres.support.PgEntityFactoryBean;
import io.github.siyukio.tools.entity.postgres.PgEntityDao;
import io.github.siyukio.tools.entity.postgres.annotation.PgEntity;
import io.github.siyukio.tools.registrar.AbstractCommonRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.lang.annotation.Annotation;

/**
 * @author Bugee
 */
@Slf4j
public class PostgresEntityRegistrar extends AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {


    @Override
    protected String getTopic() {
        return "siyukio-postgres-entity";
    }

    @Override
    protected Class<? extends Annotation> getAnnotationType() {
        return PgEntity.class;
    }

    @Override
    protected Class<?> getBeanFactoryClass() {
        return PgEntityFactoryBean.class;
    }

    @Override
    protected Class<?> getBeanClass() {
        return PgEntityDao.class;
    }

    @Override
    public void setEnvironment(Environment environment) {

    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

    }
}
