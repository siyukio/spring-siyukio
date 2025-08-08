package io.github.siyukio.tools.registrar;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Bugee
 */
public final class CommonComponentProvider extends ClassPathScanningCandidateComponentProvider {

    private final BeanDefinitionRegistry registry;

    /**
     * {@link TypeFilter} to include components to be picked up.
     * <p>
     * interfaces to consider, must not be {@literal null}.
     */
    public CommonComponentProvider(BeanDefinitionRegistry registry, Class<? extends Annotation> annotationType) {

        super(false);

        Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

        this.registry = registry;

        super.addIncludeFilter(new AnnotationTypeFilter(annotationType));
    }

    /**
     * Customizes the repository interface detection and triggers annotation
     * detection on them.
     */
    @Override
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = super.findCandidateComponents(basePackage);

        for (BeanDefinition candidate : candidates) {
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
        }

        return candidates;
    }

    /*
     * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#getRegistry()
     */
    @Override
    protected BeanDefinitionRegistry getRegistry() {
        return registry;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isIndependent() && (
                metadata.isConcrete()
                        || metadata.isInterface()
                        || metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName())
        );
    }

}
