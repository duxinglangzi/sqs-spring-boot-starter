package com.duxinglangzi.sqs.starter.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Spring 配置
 * @author wuqiong 2022/6/25
 */
public class SqsBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

    public static final String SQS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
            "com.duxinglangzi.sqs.starter.config.SqsListenerAnnotationBeanPostProcessor";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(SQS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            registry.registerBeanDefinition(SQS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
                    new RootBeanDefinition(SqsListenerAnnotationBeanPostProcessor.class));
        }
    }

}
