package io.github.siyukio.client.support;

import io.github.siyukio.client.interceptor.GzipResponseInterceptor;
import io.github.siyukio.client.interceptor.UnifiedErrorResponseInterceptor;
import io.github.siyukio.tools.api.annotation.client.ApiClient;
import io.github.siyukio.tools.util.HttpClientUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 *
 * @author Bugee
 */
public class ApiClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private final Class<?> beanClass;

    private StringValueResolver stringValueResolver;

    @Setter
    @Getter
    private boolean lazyInit = false;

    private Lazy<Object> repository;

    public ApiClientFactoryBean(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public Object getObject() throws Exception {
        return this.repository.get();
    }

    @Override
    public Class<?> getObjectType() {
        return this.beanClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.repository = Lazy.of(this::newInstance);
        if (!this.lazyInit) {
            this.repository.get();
        }
    }

    private Object newInstance() {
        ApiClient apiClient = this.beanClass.getAnnotation(ApiClient.class);

        HttpClient httpClient = HttpClientUtils.getHttpClient(apiClient.version());

        int readTimeout = apiClient.readTimeout();
        if (readTimeout <= 0) {
            readTimeout = 60;
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));

        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Accept-Encoding", "gzip")
                .requestFactory(requestFactory)
                .requestInterceptor(new UnifiedErrorResponseInterceptor())
                .requestInterceptor(new GzipResponseInterceptor())
                .messageConverters(List.of(
                        new MappingJackson2HttpMessageConverter(XDataUtils.OBJECT_MAPPER),
                        new StringHttpMessageConverter(),
                        new MappingJackson2XmlHttpMessageConverter(XDataUtils.XML_MAPPER)
                ));

        RestClient restClient = restClientBuilder.build();

        Assert.notNull(this.stringValueResolver, " stringValueResolver must not be null!");

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .embeddedValueResolver(this.stringValueResolver)
                .build();
        return factory.createClient(this.beanClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        PropertySourcesPlaceholdersResolver propertySourcesPlaceholdersResolver = new PropertySourcesPlaceholdersResolver(applicationContext.getEnvironment());
        this.stringValueResolver = strVal -> propertySourcesPlaceholdersResolver.resolvePlaceholders(strVal).toString();
    }
}
