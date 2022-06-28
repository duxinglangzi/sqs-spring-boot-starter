package com.duxinglangzi.sqs.starter.container;

import com.duxinglangzi.sqs.starter.enums.MessageDeletionPolicy;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 消息拉取的容器
 *
 * @author wuqiong 2022/6/25
 */
public class MessageListenerContainer extends AbstractMessageListenerContainer {

    private String queueUrl;
    private Method method;
    private Object bean;
    private SqsClient sqsClient;
    private MessageDeletionPolicy deletionPolicy;
    private List<QueueAttributeName> attributeNames;
    private List<String> messageAttributeNames;

    public MessageListenerContainer(
            String queueUrl, List<QueueAttributeName> attributeNames, List<String> messageAttributeNames,
            MessageDeletionPolicy deletionPolicy, Method method, Object bean, SqsClient sqsClient) {
        this.queueUrl = queueUrl;
        this.attributeNames = attributeNames;
        this.messageAttributeNames = messageAttributeNames;
        this.deletionPolicy = deletionPolicy;
        this.method = method;
        this.bean = bean;
        this.sqsClient = sqsClient;
    }

    public void doStart() {
        try {
            ReceiveMessageRequest.Builder requestBuilder = ReceiveMessageRequest.builder().queueUrl(queueUrl);
            requestBuilder.maxNumberOfMessages(10);
            if (attributeNames != null) requestBuilder.attributeNames(attributeNames);
            if (messageAttributeNames != null) requestBuilder.messageAttributeNames(messageAttributeNames);
            ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(requestBuilder.build());
            if (receiveMessageResponse.hasMessages()) {
                for (Message message : receiveMessageResponse.messages()) {
                    Exception exception = null;
                    try {
                        // NEVER 策略时,方法参数不同
                        if (MessageDeletionPolicy.NEVER == deletionPolicy) {
                            method.invoke(bean, message, createAck(message));
                        } else {
                            method.invoke(bean, message);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        exception = e;
                        e.printStackTrace();
                    }
                    switch (deletionPolicy) {
                        case ALWAYS:
                            deleteMessage(message.receiptHandle());
                        case SUCCESS:
                            if (exception == null) deleteMessage(message.receiptHandle());
                    }
                }
            } else {
                sleep(SLEEP_TIME_MILLI_SECONDS);
            }
        } catch (Exception exc) {
            // 防止删除消息时发生错误,或者拉取消息失败等情况
            exc.printStackTrace();
        }
    }

    private QueueMessageAcknowledgment createAck(Message message) {
        return new QueueMessageAcknowledgment(queueUrl, message.receiptHandle(), sqsClient);
    }

    private boolean deleteMessage(String receiptHandle) {
        DeleteMessageRequest buildDeleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        boolean isDelete = sqsClient.deleteMessage(buildDeleteRequest).sdkHttpResponse().isSuccessful();
        if (!isDelete) throw new RuntimeException("Message Cannot delete ,receiptHandle: " + receiptHandle);
        return true;
    }

}
