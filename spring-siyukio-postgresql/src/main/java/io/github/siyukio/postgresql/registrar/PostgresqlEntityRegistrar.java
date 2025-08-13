package io.github.siyukio.postgresql.registrar;

import io.github.siyukio.postgresql.support.PgEntityFactoryBean;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.postgresql.annotation.PgEntity;
import io.github.siyukio.tools.registrar.AbstractCommonRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;

import java.lang.annotation.Annotation;

/**
 * @author Bugee
 */
@Slf4j
public class PostgresqlEntityRegistrar extends AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

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
}
