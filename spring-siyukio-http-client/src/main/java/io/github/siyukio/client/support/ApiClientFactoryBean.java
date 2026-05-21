package io.github.siyukio.client.support;

import io.github.siyukio.client.interceptor.*;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.annotation.client.ApiClient;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.HttpClientUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.StringValueResolver;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory bean for creating API client proxy instances.
 *
 * @author Bugee
 */
@Slf4j
public class ApiClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private final Class<?> beanClass;

    private PropertySourcesPlaceholdersResolver propertySourcesPlaceholdersResolver;

    private AipHandlerManager aipHandlerManager;

    private TokenProvider tokenProvider;

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
        if (!apiClient.url().contains("${")) {
            log.info("Api client: {} -> {}", beanClass.getSimpleName(), apiClient.url());
        }

        HttpClient httpClient = HttpClientUtils.getHttpClient(apiClient.version());

        int readTimeout = apiClient.readTimeout();
        if (readTimeout <= 0) {
            readTimeout = 60;
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));

        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setSupportedMediaTypes(List.of(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_JSON,
                MediaType.ALL
        ));

        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Accept-Encoding", "gzip, br")
                .requestFactory(requestFactory)
                .requestInterceptor(new UnifiedErrorResponseInterceptor())
                .requestInterceptor(new GzipResponseInterceptor())
                .requestInterceptor(new BrotliResponseInterceptor())
                .messageConverters(List.of(
                        stringHttpMessageConverter,
                        new MappingJackson2HttpMessageConverter(XDataUtils.OBJECT_MAPPER),
                        new MappingJackson2XmlHttpMessageConverter(XDataUtils.XML_MAPPER),
                        new FormHttpMessageConverter()
                ));

        if (apiClient.loadBalance()) {
            restClientBuilder.requestInterceptor(new LoadBalanceInterceptor());
        }

        if (this.aipHandlerManager != null && this.tokenProvider != null) {
            restClientBuilder.requestInterceptor(new LocalRequestInterceptor(this.aipHandlerManager, this.tokenProvider));
        }

        RestClient restClient = restClientBuilder.build();

        AtomicReference<Boolean> logUrlReference = new AtomicReference<>(false);
        StringValueResolver stringValueResolver = strVal -> {
            String value = propertySourcesPlaceholdersResolver.resolvePlaceholders(strVal).toString();
            if (apiClient.url().equals(strVal)) {
                Boolean logUrl = logUrlReference.get();
                if (!logUrl) {
                    logUrlReference.set(true);
                    log.info("Api client use resolver: {} -> {}", beanClass.getSimpleName(), value);
                }
            }
            return value;
        };

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .embeddedValueResolver(stringValueResolver)
                .build();
        return factory.createClient(this.beanClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.propertySourcesPlaceholdersResolver = new PropertySourcesPlaceholdersResolver(applicationContext.getEnvironment());
        ObjectProvider<AipHandlerManager> aipHandlerManagerProvider = applicationContext.getBeanProvider(AipHandlerManager.class);
        this.aipHandlerManager = aipHandlerManagerProvider.getIfAvailable();
        ObjectProvider<TokenProvider> tokenProviderProvider = applicationContext.getBeanProvider(TokenProvider.class);
        this.tokenProvider = tokenProviderProvider.getIfAvailable();
    }
}
