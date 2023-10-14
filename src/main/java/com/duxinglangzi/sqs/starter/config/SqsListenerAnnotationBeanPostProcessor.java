package com.duxinglangzi.sqs.starter.config;

import com.duxinglangzi.sqs.starter.annotation.SqsListener;
import com.duxinglangzi.sqs.starter.common.Constants;
import com.duxinglangzi.sqs.starter.factory.SqsEndpointFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对sqs listener 进行扫描以及分析
 *
 * @author wuqiong 2022/6/25
 */
public class SqsListenerAnnotationBeanPostProcessor implements
        BeanPostProcessor, SmartInitializingSingleton, BeanFactoryPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SqsListenerAnnotationBeanPostProcessor.class);
    private final Set<Class<?>> notAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
    private Set<SqsListenerEndpointRegistrar> registrars = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private AtomicInteger ATOMIC_INTEGER = new AtomicInteger();
    private final String CONTAINER_ID_PREFIX = "com.duxinglangzi.sqs.starter.config.SqsListenerEndpointRegistrar#";
    private ConfigurableListableBeanFactory configurableListableBeanFactory;
    private AsyncTaskExecutor asyncTaskExecutor;
    private SqsConfig sqsConfig;
    private ApplicationContext applicationContext;


    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        if (notAnnotatedClasses.contains(bean.getClass())) return bean;
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Map<Method, SqsListener> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<SqsListener>) method -> findListenerAnnotations(method));
        if (annotatedMethods.isEmpty()) {
            this.notAnnotatedClasses.add(bean.getClass());
        } else {
            annotatedMethods.entrySet().stream().filter(e -> e != null).forEach(
                    ele -> registrars.add(new SqsListenerEndpointRegistrar(
                            bean, ele, getContainerID(), applicationContext.getEnvironment())));
            logger.info("Registered @SqsListener methods processed on bean:{} , Methods :{} ", bean.getClass().getName(),
                    annotatedMethods.keySet().stream().map(e -> e.getName()).collect(Collectors.toSet()));
        }
        return bean;
    }

    private String getContainerID() {
        return CONTAINER_ID_PREFIX + ATOMIC_INTEGER.getAndIncrement();
    }

    private SqsListener findListenerAnnotations(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, SqsListener.class);
    }


    @Override
    public void afterSingletonsInstantiated() {
        sqsConfig = configurableListableBeanFactory.getBean(SqsConfig.class);
        SqsEndpointFactory.createBatchByConfig(sqsConfig);
        if (this.registrars.isEmpty()) return;
        Map<String, Long> registrarsMap = this.registrars.stream().map(each -> each.getListenerEntry().getValue().clientName())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        asyncTaskExecutor = createDefaultTaskExecutor(registrarsMap);
        this.registrars.forEach(e -> e.registerListenerContainer(configurableListableBeanFactory, sqsConfig, asyncTaskExecutor));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        this.configurableListableBeanFactory = configurableListableBeanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // 不注册到spring 是怕有人在使用过程中，使用了这个线程池,导致积压消息
    protected AsyncTaskExecutor createDefaultTaskExecutor(Map<String, Long> registrarsMap) {
        int corePoolSize = registrarsMap.values().stream().collect(Collectors.summingInt(l -> l.intValue()));
        int maxPoolSize = corePoolSize * Constants.DEFAULT_BATCH_MESSAGE;
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("AsyncTaskExecutor_SQS_Listener - ");
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setQueueCapacity(corePoolSize);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }
}
