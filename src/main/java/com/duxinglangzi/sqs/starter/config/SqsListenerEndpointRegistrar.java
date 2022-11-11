package com.duxinglangzi.sqs.starter.config;

import com.duxinglangzi.sqs.starter.annotation.SqsListener;
import com.duxinglangzi.sqs.starter.container.MessageListenerContainer;
import com.duxinglangzi.sqs.starter.container.QueueMessageAcknowledgment;
import com.duxinglangzi.sqs.starter.enums.MessageDeletionPolicy;
import com.duxinglangzi.sqs.starter.factory.SqsEndpointFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * sqs listener 终端注册器
 *
 * @author wuqiong 2022/6/25
 */
public class SqsListenerEndpointRegistrar {

    private Object bean;
    private String registerID;
    private Environment environment;
    private Map.Entry<Method, SqsListener> listenerEntry;

    public void registerListenerContainer(ConfigurableListableBeanFactory beanFactory, SqsConfig sqsConfig, AsyncTaskExecutor asyncTaskExecutor) {
        if (beanFactory.containsBean(getRegisterID())) return; // 如果已经存在则不在创建
        SqsClient sqsClient = SqsEndpointFactory.getSqsClient(getListenerEntry().getValue().clientName());
        if (sqsClient == null) {
            if (!sqsConfig.getInstances().containsKey(getListenerEntry().getValue().clientName())) {
                // 没有找到默认的连接, 请参考 SqsConfig 内的注解示例
                throw new IllegalArgumentException("@SqsListener defaultClientName not found , please check ");
            }
            sqsClient = SqsEndpointFactory.createSqsClient(getListenerEntry().getValue().clientName(),
                    sqsConfig.getInstances().get(getListenerEntry().getValue().clientName()));
        }

        List<Class<?>> parameterTypes = parameterTypes();
        if (!parameterTypes.contains(Message.class)) {
            throw new IllegalArgumentException(
                    "@SqsListener method not parameter type : software.amazon.awssdk.services.sqs.model.Message , please check ");
        }
        if (MessageDeletionPolicy.NEVER == getListenerEntry().getValue().deletionPolicy() &&
                !parameterTypes.contains(QueueMessageAcknowledgment.class)) {
            throw new IllegalArgumentException(
                    "@SqsListener method not parameter type : com.duxinglangzi.sqs.starter.container.QueueMessageAcknowledgment , please check ");
        }
        beanFactory.registerSingleton(getRegisterID(), new MessageListenerContainer(
                environment.resolvePlaceholders(getListenerEntry().getValue().queueUrl()),
                attributeNameList(getListenerEntry().getValue().attributeNames()),
                new ArrayList<>(Arrays.asList(getListenerEntry().getValue().messageAttributeNames())),
                getListenerEntry().getValue().deletionPolicy(),
                getListenerEntry().getKey(),
                getBean(),
                sqsClient,
                asyncTaskExecutor
        ));
    }

    private List<QueueAttributeName> attributeNameList(String[] attributeNames) {
        if (attributeNames == null || attributeNames.length == 0) return null;
        return Arrays.asList(attributeNames).stream().map(e -> QueueAttributeName.fromValue(e)).collect(Collectors.toList());
    }

    private List<Class<?>> parameterTypes() {
        return Arrays.stream(listenerEntry.getKey().getParameterTypes()).collect(Collectors.toList());
    }

    public SqsListenerEndpointRegistrar(Object bean, Map.Entry<Method, SqsListener> entry, String registerID, Environment environment) {
        this.bean = bean;
        this.listenerEntry = entry;
        this.registerID = registerID;
        this.environment = environment;
    }

    public String getRegisterID() {
        return registerID;
    }

    public Map.Entry<Method, SqsListener> getListenerEntry() {
        return listenerEntry;
    }

    public Object getBean() {
        return bean;
    }
}
