package com.siyukio.application.boot.starter.autoconfigure;

import com.siyukio.application.interceptor.ValidateAuthorizationInterceptor;
import com.siyukio.application.interceptor.ValidateParameterInterceptor;
import com.siyukio.application.interceptor.ValidateSignatureInterceptor;
import com.siyukio.application.method.*;
import com.siyukio.tools.api.AipHandlerManager;
import com.siyukio.tools.api.ApiMock;
import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.definition.ApiDefinitionManager;
import com.siyukio.tools.api.signature.SignatureProvider;
import com.siyukio.tools.api.token.TokenProvider;
import com.siyukio.tools.util.JsonUtils;
import com.siyukio.tools.util.KeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Buddy
 */
@Slf4j
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
public class MyWebMvcConfiguration extends WebMvcConfigurationSupport {

    @Bean
    @Nullable
    @Override
    public HandlerMapping resourceHandlerMapping(
            @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
            @Qualifier("mvcConversionService") FormattingConversionService conversionService,
            @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
        log.info("disabled resourceHandlerMapping...");
        return null;
    }

    @Bean
    public ApiDefinitionManager apiDefinitionManager() {
        ApiDefinitionManager apiDefinitionManager = new ApiDefinitionManager();
        apiDefinitionManager.addAlternative(DeferredResult.class);
        return apiDefinitionManager;
    }

    @Bean
    public AipHandlerManager aipHandlerManager() {
        return new AipHandlerManager();
    }

    @Bean
    public ApiMock apiMock() {
        return new ApiMock();
    }

    @Bean
    public TokenProvider tokenProvider() {
        assert this.getApplicationContext() != null;
        Environment env = this.getApplicationContext().getEnvironment();

        String publicKey = env.getProperty(ApiConstants.PROPERTY_API_JWT_PUBLIC, "");
        RSAPublicKey rsaPublicKey = null;
        if (StringUtils.hasText(publicKey)) {
            try {
                rsaPublicKey = KeyUtils.getPublicKeyFromPem(publicKey);
            } catch (Exception e) {
                log.error("getPublicKeyFromPem error: {}", publicKey, e);
            }
        }

        String privateKey = env.getProperty(ApiConstants.PROPERTY_API_JWT_PRIVATE, "");
        RSAPrivateKey rsaPrivateKey = null;
        if (StringUtils.hasText(privateKey)) {
            try {
                rsaPrivateKey = KeyUtils.getPrivateKeyFromPem(privateKey);
            } catch (Exception e) {
                log.error("getPrivateKeyFromPem error: {}", privateKey, e);
            }
        }
        String accessTokenDuration = env.getProperty(ApiConstants.PROPERTY_API_JWT_ACCESS_TOKEN_DURATION, "PT15M");
        Duration accessDuration = Duration.parse(accessTokenDuration);

        String refreshTokenDuration = env.getProperty(ApiConstants.PROPERTY_API_JWT_REFRESH_TOKEN_DURATION, "P30D");
        Duration refreshDuration = Duration.parse(refreshTokenDuration);

        log.info("init TokenProvider, accessTokenDuration:{} refreshTokenDuration:{}", accessTokenDuration, refreshTokenDuration);

        return new TokenProvider(rsaPublicKey, rsaPrivateKey, accessDuration, refreshDuration);
    }

    @Bean
    public SignatureProvider signatureProvider() {
        assert this.getApplicationContext() != null;
        Environment env = this.getApplicationContext().getEnvironment();

        String salt = env.getProperty(ApiConstants.PROPERTY_API_SIGNATURE_SALT, "");
        log.info("init SignatureProvider, salt:{}", salt);
        return new SignatureProvider(salt);
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        for (HandlerExceptionResolver resolver : resolvers) {
            if (resolver instanceof ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver) {
                HandlerMethodReturnValueHandlerComposite composite = exceptionHandlerExceptionResolver.getReturnValueHandlers();
                assert composite != null;
                List<HandlerMethodReturnValueHandler> newReturnValueHandlers = new ArrayList<>(composite.getHandlers().size());
                for (HandlerMethodReturnValueHandler originReturnValueHandler : composite.getHandlers()) {
                    if (originReturnValueHandler instanceof RequestResponseBodyMethodProcessor) {
                        log.info("use ExceptionReturnValueHandler override RequestResponseBodyMethodProcessor for exception");
                        ExceptionReturnValueHandler exceptionReturnValueHandler = new ExceptionReturnValueHandler(originReturnValueHandler);
                        newReturnValueHandlers.add(exceptionReturnValueHandler);
                    } else {
                        newReturnValueHandlers.add(originReturnValueHandler);
                    }
                }
                exceptionHandlerExceptionResolver.setReturnValueHandlers(newReturnValueHandlers);
            }
        }
    }


    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        ApplicationContext applicationContext = this.getApplicationContext();

        assert applicationContext != null;
        AipHandlerManager aipHandlerManager = applicationContext.getBean(AipHandlerManager.class);
        SignatureProvider signatureProvider = applicationContext.getBean(SignatureProvider.class);

        registry.addInterceptor(new ValidateSignatureInterceptor(aipHandlerManager, signatureProvider)).addPathPatterns("/**").order(5);
        log.info("init ValidateSignatureInterceptor");

        registry.addInterceptor(new ValidateParameterInterceptor(aipHandlerManager)).addPathPatterns("/**").order(6);
        log.info("init ValidateParameterInterceptor");

        TokenProvider tokenProvider = applicationContext.getBean(TokenProvider.class);
        registry.addInterceptor(new ValidateAuthorizationInterceptor(aipHandlerManager, tokenProvider)).addPathPatterns("/**").order(7);
        log.info("init ValidateTokenInterceptor");
    }

    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        HttpMessageConverter<?> converter;
        for (int index = 0; index < converters.size(); index++) {
            converter = converters.get(index);
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                log.info("override MappingJackson2HttpMessageConverter support JsonOrgModule,JavaTimeModule");
                converters.set(index, new MappingJackson2HttpMessageConverter(JsonUtils.getObjectMapper()));
            }
        }
    }

    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

        log.info("init TokenArgumentResolver");
        argumentResolvers.add(new TokenArgumentResolver());

        log.info("init RequestArgumentResolver");
        argumentResolvers.add(new RequestArgumentResolver());
    }

    @Bean
    public InitializingBean feignInitializingBean() {
        return new InitializingBean() {

            @Autowired
            private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

            @Autowired
            private ApplicationContext applicationContext;

            @Override
            public void afterPropertiesSet() throws Exception {
                List<HandlerMethodArgumentResolver> originArgumentResolvers = this.requestMappingHandlerAdapter.getArgumentResolvers();
                Assert.notNull(originArgumentResolvers, "RequestMappingHandlerAdapter.getArgumentResolvers is not initialized");
                List<HandlerMethodArgumentResolver> newArgumentResolvers = new ArrayList<>(originArgumentResolvers.size());
                for (HandlerMethodArgumentResolver originArgumentResolver : originArgumentResolvers) {
                    if (originArgumentResolver instanceof RequestResponseBodyMethodProcessor) {
                        log.info("use SkipRequestResponseBodyMethodProcessor override RequestResponseBodyMethodProcessor, skip @RequestBody");
                        SkipRequestResponseBodyMethodProcessor skipRequestResponseBodyMethodProcessor = new SkipRequestResponseBodyMethodProcessor(originArgumentResolver);
                        newArgumentResolvers.add(skipRequestResponseBodyMethodProcessor);
                    } else {
                        newArgumentResolvers.add(originArgumentResolver);
                    }
                }
                this.requestMappingHandlerAdapter.setArgumentResolvers(newArgumentResolvers);

                List<HandlerMethodReturnValueHandler> originReturnValueHandlers = this.requestMappingHandlerAdapter.getReturnValueHandlers();
                Assert.notNull(originReturnValueHandlers, "RequestMappingHandlerAdapter.getReturnValueHandlers is not initialized");
                List<HandlerMethodReturnValueHandler> newReturnValueHandlers = new ArrayList<>(originReturnValueHandlers.size());
                for (HandlerMethodReturnValueHandler originReturnValueHandler : originReturnValueHandlers) {
                    if (originReturnValueHandler instanceof RequestResponseBodyMethodProcessor) {
                        log.info("use JsonReturnValueHandler override RequestResponseBodyMethodProcessor for @ResponseBody");
                        AipHandlerManager aipHandlerManager = this.applicationContext.getBean(AipHandlerManager.class);
                        ApiReturnValueHandler ApiReturnValueHandler = new ApiReturnValueHandler(aipHandlerManager, originReturnValueHandler);
                        newReturnValueHandlers.add(ApiReturnValueHandler);
                    } else {
                        newReturnValueHandlers.add(originReturnValueHandler);
                    }
                }
                this.requestMappingHandlerAdapter.setReturnValueHandlers(newReturnValueHandlers);
            }
        };
    }

}
