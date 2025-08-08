package io.github.siyukio.postgres.support;

import io.github.siyukio.tools.entity.postgres.PgEntityDao;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;

/**
 * @author Bugee
 */
@Slf4j
public class PgEntityFactoryBean implements FactoryBean<PgEntityDao<?>>, InitializingBean, ApplicationContextAware {

    private final Class<?> entityClass;
    @Getter
    @Setter
    private boolean lazyInit = false;

    private Lazy<PgEntityDao<?>> repository;

    private ApplicationContext applicationContext;

    public PgEntityFactoryBean(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public PgEntityDao<?> getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return PgEntityDao.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.repository = Lazy.of(this::newInstance);
        if (!this.lazyInit) {
            this.repository.get();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private PgEntityDao<?> newInstance() {
        //todo
        throw new UnsupportedOperationException();
    }
}
