package io.github.siyukio.client.support;

import io.github.siyukio.client.interceptor.BrotliResponseInterceptor;
import io.github.siyukio.client.interceptor.DnsUrlRewriteInterceptor;
import io.github.siyukio.client.interceptor.GzipResponseInterceptor;
import io.github.siyukio.client.interceptor.UnifiedErrorResponseInterceptor;
import io.github.siyukio.tools.api.annotation.client.ApiClient;
import io.github.siyukio.tools.api.annotation.client.DnsResolver;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Factory bean for creating API client proxy instances.
 *
 * @author Bugee
 */
@Slf4j
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

        String baseUrl = apiClient.url();

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(apiClient.version())
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL);

        DnsResolver dnsResolver = apiClient.dnsResolver();
        DnsUrlRewriteInterceptor dnsUrlRewriteInterceptor = null;

        if (dnsResolver != null) {
            String dns = dnsResolver.dns();
            if (StringUtils.hasText(dns) && StringUtils.hasText(baseUrl)) {
                String originalHost = URI.create(baseUrl).getHost();
                dnsUrlRewriteInterceptor = new DnsUrlRewriteInterceptor(dns,
                        dnsResolver.port(), dnsResolver.useTcp(), originalHost);
                log.debug("DNS rewrite interceptor created for host: {}, dns: {}:{}, useTcp: {}",
                        originalHost, dns, dnsResolver.port(), dnsResolver.useTcp());
            }
        }

        HttpClient httpClient = httpClientBuilder.build();

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
        if (dnsUrlRewriteInterceptor != null) {
            restClientBuilder.requestInterceptor(dnsUrlRewriteInterceptor);
        }

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
