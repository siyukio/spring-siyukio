package com.siyukio.application.boot.starter.autoconfigure;

import com.siyukio.tools.api.AipHandlerManager;
import com.siyukio.tools.api.ApiProfiles;
import com.siyukio.tools.api.annotation.ApiController;
import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.definition.ApiDefinitionManager;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.DispatcherServlet;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author Buddy
 */
@Slf4j
@AutoConfigureAfter({WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class})
@ComponentScan("com.siyukio.application")
public class MyApplicationConfiguration {

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private DispatcherServlet dispatcherServlet;

    @Autowired
    private ApiDefinitionManager apiDefinitionManager;

    @Autowired
    private AipHandlerManager apiHandlerManager;

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        ApiProfiles.PORT = event.getWebServer().getPort();
        log.info("ApiProfiles port:{}", ApiProfiles.PORT);
    }

    @PostConstruct
    public void init() {
        try {
            //
            InetAddress ip4 = InetAddress.getLocalHost();
            ApiProfiles.IP4 = ip4.getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        log.info("ApiProfiles ip4:{}", ApiProfiles.IP4);

        String contextPath = this.servletContext.getContextPath();
        if (contextPath != null && !contextPath.equals("/")) {
            ApiProfiles.CONTEXT_PATH = contextPath;
        }
        log.info("ApiProfiles contextPath:{}", ApiProfiles.CONTEXT_PATH);

        this.initApi();
    }

    private Class<?> getTargetClass(Object proxy) {
        Class<?> clazz = proxy.getClass();
        if (AopUtils.isAopProxy(proxy)) {
            clazz = AopUtils.getTargetClass(proxy);
        }
        return clazz;
    }

    private void initApi() {

        Map<String, Object> apiControllerBeanMap = this.applicationContext.getBeansWithAnnotation(ApiController.class);
        Class<?> apiControllerClass;
        Method[] methodArray;
        ApiDefinition apiDefinition;
        for (Map.Entry<String, Object> entry : apiControllerBeanMap.entrySet()) {
            Object bean = entry.getValue();
            apiControllerClass = this.getTargetClass(bean);
            methodArray = apiControllerClass.getMethods();
            for (Method method : methodArray) {
                if (!Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                    if (this.apiDefinitionManager.isApi(apiControllerClass, method)) {
                        apiDefinition = this.apiDefinitionManager.addApi(apiControllerClass, method);
                        this.apiHandlerManager.addApiHandler(apiDefinition, bean, method);
                    }
                }
            }
        }
    }

}
