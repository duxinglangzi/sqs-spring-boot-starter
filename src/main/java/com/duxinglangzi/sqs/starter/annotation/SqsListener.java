package com.duxinglangzi.sqs.starter.annotation;


import com.duxinglangzi.sqs.starter.enums.MessageDeletionPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sqs listener 用于监听指定队列消息,仅能作用于方法上
 *
 * @author wuqiong 2022/6/25
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SqsListener {

    /**
     * 队列地址
     */
    String queueUrl();

    /**
     * 连接的名称
     * <p>
     * 如果不存在，则会将配置的第一个设置成默认的.
     * <p>
     * 此处主要为了解决系统内,存在多个不同地区或账户的sqs服务.
     */
    String clientName() default "defaults";

    /**
     * 属性名称
     */
    String[] attributeNames() default "";

    /**
     * 待获取的消息的属性名称
     */
    String[] messageAttributeNames() default "";

    /**
     * 默认情况下，只有消息成功且没有引发异常则删除消息
     */
    MessageDeletionPolicy deletionPolicy() default MessageDeletionPolicy.SUCCESS;

    /**
     * 默认每次拉取10条数据
     */
    int maxNumberOfMessages() default 10;

}
