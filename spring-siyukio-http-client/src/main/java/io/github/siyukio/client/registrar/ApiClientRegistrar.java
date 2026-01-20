package io.github.siyukio.client.registrar;

import io.github.siyukio.client.support.ApiClientFactoryBean;
import io.github.siyukio.tools.api.annotation.client.ApiClient;
import io.github.siyukio.tools.registrar.AbstractCommonRegistrar;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;

import java.lang.annotation.Annotation;

/**
 *
 * @author Bugee
 */
public class ApiClientRegistrar extends AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    @Override
    protected String getTopic() {
        return "siyukio-api-client";
    }

    @Override
    protected Class<? extends Annotation> getAnnotationType() {
        return ApiClient.class;
    }

    @Override
    protected Class<?> getBeanFactoryClass() {
        return ApiClientFactoryBean.class;
    }

    @Override
    protected Class<?> getBeanClass() {
        return null;
    }
}
