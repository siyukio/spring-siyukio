package io.github.siyukio.tools.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Bugee
 */
@Slf4j
public abstract class AbstractCommonRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private final static Set<String> basePackageSet = new HashSet<>();
    protected Environment environment;
    private ResourceLoader resourceLoader;

    protected abstract String getTopic();

    protected abstract Class<? extends Annotation> getAnnotationType();

    protected abstract Class<?> getBeanFactoryClass();

    protected abstract Class<?> getBeanClass();

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerBeanDefinitions(metadata, registry, ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR);
    }

    private Set<String> getScanBasePackages(AnnotationMetadata annotationMetadata) {
        Set<String> basePackageSet = new HashSet<>();
        Map<String, Object> attrMap = annotationMetadata.getAnnotationAttributes("org.springframework.context.annotation.ComponentScan");
        if (attrMap != null) {
            String[] valueArray = (String[]) attrMap.get("value");
            for (String value : valueArray) {
                if (!value.isEmpty()) {
                    basePackageSet.add(value);
                }
            }
        }
        return basePackageSet;
    }

    /*
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator generator) {
        String topic = this.getTopic();
        Assert.notNull(metadata, topic + " AnnotationMetadata must not be null!");
        Assert.notNull(registry, topic + " BeanDefinitionRegistry must not be null!");
        Assert.notNull(resourceLoader, topic + " ResourceLoader must not be null!");
        Assert.notNull(environment, topic + " Environment must not be null!");

        //SpringBootApplication package
        if (basePackageSet.isEmpty()) {
            String bootPackage = null;
            AnnotatedBeanDefinition abd;
            AnnotationMetadata annotationMetadata;
            String[] beanNames = registry.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
                if (beanDefinition instanceof AnnotatedBeanDefinition) {
                    abd = (AnnotatedBeanDefinition) beanDefinition;
                    annotationMetadata = abd.getMetadata();
                    if (annotationMetadata.getAnnotationTypes().contains("org.springframework.boot.autoconfigure.SpringBootApplication")) {
                        //find SpringBootApplication
                        bootPackage = ClassUtils.getPackageName(annotationMetadata.getClassName());
                        basePackageSet.add(bootPackage);
                    } else if (annotationMetadata.getAnnotationTypes().contains("org.springframework.context.annotation.ComponentScan")) {
                        basePackageSet.addAll(this.getScanBasePackages(annotationMetadata));
                    }
                }
            }
            Assert.notNull(bootPackage, topic + " @SpringBootApplication not found!");
            log.info("Bootstrapping find basePackages:{}", basePackageSet);
        }

        //scan package
        CommonBeanDefinitionBuilder commonBeanDefinitionBuilder = new CommonBeanDefinitionBuilder(metadata, resourceLoader, this.getBeanFactoryClass(), this.getBeanClass());
        CommonComponentProvider commonComponentProvider = new CommonComponentProvider(registry, this.getAnnotationType());
        BeanDefinitionBuilder beanDefinitionBuilder;
        AbstractBeanDefinition beanDefinition;
        String beanName;
        Set<BeanDefinition> scannedDefinitionSet;
        Set<BeanDefinition> allScannedDefinitionSet = new HashSet<>();
        for (String value : basePackageSet) {
            if (!value.isEmpty()) {
                scannedDefinitionSet = commonComponentProvider.findCandidateComponents(value);
                allScannedDefinitionSet.addAll(scannedDefinitionSet);
            }
        }
        log.info("Bootstrapping {} find {} repositories.", topic, allScannedDefinitionSet.size());
        for (BeanDefinition scannedDefinition : allScannedDefinitionSet) {
            beanDefinitionBuilder = commonBeanDefinitionBuilder.build(scannedDefinition);
            beanDefinition = beanDefinitionBuilder.getBeanDefinition();
            beanName = this.buildDefaultBeanName(scannedDefinition);
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private String buildDefaultBeanName(BeanDefinition definition) {
        String beanClassName = definition.getBeanClassName();
        Assert.state(beanClassName != null, "No bean class name set");
        String shortClassName = ClassUtils.getShortName(beanClassName);
        return Introspector.decapitalize(shortClassName);
    }

}
