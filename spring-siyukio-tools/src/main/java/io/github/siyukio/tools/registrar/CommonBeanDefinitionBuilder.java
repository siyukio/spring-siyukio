package io.github.siyukio.tools.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Bugee
 */
@Slf4j
public final class CommonBeanDefinitionBuilder {

    private final AnnotationMetadata configMetadata;

    private final ResourceLoader resourceLoader;

    private final Class<?> beanFactoryClass;

    private final Class<?> beanClass;


    /**
     * @param configMetadata
     * @param resourceLoader
     * @param beanFactoryClass
     * @param beanClass
     */
    public CommonBeanDefinitionBuilder(AnnotationMetadata configMetadata, ResourceLoader resourceLoader, Class<?> beanFactoryClass, Class<?> beanClass) {
        this.beanFactoryClass = beanFactoryClass;
        this.beanClass = beanClass;
        Assert.notNull(configMetadata, "AnnotationMetadata must not be null!");
        Assert.notNull(configMetadata, "ResourceLoader must not be null!");

        this.configMetadata = configMetadata;
        this.resourceLoader = resourceLoader;
    }

    /**
     * @param scannedDefinition
     * @return
     */
    public BeanDefinitionBuilder build(BeanDefinition scannedDefinition) {

        Assert.notNull(scannedDefinition, "BeanDefinition must not be null!");

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(this.beanFactoryClass);
        builder.getRawBeanDefinition().setSource(this.configMetadata);
        builder.addConstructorArgValue(scannedDefinition.getBeanClassName());
        builder.addPropertyValue("lazyInit", false);
        builder.setLazyInit(false);
        builder.setPrimary(false);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        String simpleClassName = ClassUtils.getShortName(configMetadata.getClassName());
        String resourceDescription = String.format("%s defined in %s", scannedDefinition.getBeanClassName(), simpleClassName);
//        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, scannedDefinition.getBeanClassName());
        beanDefinition.setResourceDescription(resourceDescription);

        this.initTargetType(beanDefinition, scannedDefinition.getBeanClassName());
        return builder;
    }

    private void initTargetType(AbstractBeanDefinition beanDefinition, String scannedBeanClassName) {
        if (this.beanClass != null) {
            //Set generic type for dependency injection.
            Class<?> scannedBeanClass;
            try {
                scannedBeanClass = ClassUtils.forName(scannedBeanClassName, this.resourceLoader.getClassLoader());
                RootBeanDefinition rbd = (RootBeanDefinition) beanDefinition;
                ResolvableType targetType;
                targetType = ResolvableType.forClassWithGenerics(this.beanClass, scannedBeanClass);
                rbd.setTargetType(targetType);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
    }

}
