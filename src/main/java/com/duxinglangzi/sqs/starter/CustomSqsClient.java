package com.duxinglangzi.sqs.starter;

import com.duxinglangzi.sqs.starter.factory.SqsEndpointFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Map;

/**
 * sqs 连接
 *
 * @author wuqiong 2022/6/25
 */
public class CustomSqsClient {

    /**
     * 发送标准消息队列的消息
     *
     * @param queueUrl    队列url地址
     * @param messageBody 消息内容
     * @return SendMessageResponse
     * @author wuqiong 2022/8/5 13:58
     */
    public static SendMessageResponse sentStandardMessage(String queueUrl, String messageBody) {
        return sentStandardMessage(null, queueUrl, messageBody, null, null);
    }

    /**
     * 发送标准消息队列的消息
     *
     * @param clientName        连接名称 , 为空则使用 defaults
     * @param queueUrl          队列url地址
     * @param messageBody       消息内容
     * @param delaySeconds      延迟秒数
     * @param messageAttributes 消息自定义参数
     * @return SendMessageResponse
     * @author wuqiong 2022/6/25 15:44
     */
    public static SendMessageResponse sentStandardMessage(String clientName, String queueUrl, String messageBody, Integer delaySeconds, Map<String, MessageAttributeValue> messageAttributes) {
        return SqsEndpointFactory.getSqsClient(clientName).sendMessage(createBuilder(queueUrl, messageBody, delaySeconds, messageAttributes).build());
    }

    /**
     * 发送 FIFO 队列的消息
     *
     * @param queueUrl               队列url地址
     * @param messageBody            消息内容
     * @param messageGroupId         消息组ID, FIFO队列此值必需存在
     * @param messageDeduplicationId 消息重复ID, FIFO队列此值必需存在
     * @return SendMessageResponse
     * @author wuqiong 2022/8/5 14:00
     */
    public static SendMessageResponse sentFifoMessage(String queueUrl, String messageBody, String messageGroupId, String messageDeduplicationId) {
        return sentFifoMessage(null, queueUrl, messageBody, messageGroupId, messageDeduplicationId, null, null);
    }

    /**
     * 发送 FIFO 队列的消息
     *
     * @param clientName             连接名称 , 为空则使用 defaults
     * @param queueUrl               队列url地址
     * @param messageBody            消息内容
     * @param delaySeconds           延迟秒数
     * @param messageGroupId         消息组ID, FIFO队列此值必需存在
     * @param messageDeduplicationId 消息重复ID, FIFO队列此值必需存在
     * @param messageAttributes      消息自定义参数
     * @return SendMessageResponse
     * @author wuqiong 2022/6/25 15:49
     */
    public static SendMessageResponse sentFifoMessage(String clientName, String queueUrl, String messageBody, String messageGroupId, String messageDeduplicationId, Integer delaySeconds, Map<String, MessageAttributeValue> messageAttributes) {
        SendMessageRequest.Builder builder = createBuilder(queueUrl, messageBody, delaySeconds, messageAttributes);
        Assert.hasText(messageGroupId, "参数 messageGroupId 值不能为空,请检查");
        Assert.hasText(messageDeduplicationId, "参数 messageDeduplicationId 值不能为空,请检查");
        builder.messageGroupId(messageGroupId);
        builder.messageDeduplicationId(messageDeduplicationId);
        return SqsEndpointFactory.getSqsClient(clientName).sendMessage(builder.build());
    }


    /**
     * 拉取一个 或者 多个消息
     *
     * @param clientName          连接名称 , 为空则使用 defaults
     * @param queueUrl            队列url地址
     * @param attributeNames      需要返回的参数
     * @param maxNumberOfMessages 拉取条数，最多10条，默认1条
     * @return ReceiveMessageResponse
     * @author wuqiong 2022/6/25 15:13
     */
    public static ReceiveMessageResponse receiveMessage(String clientName, String queueUrl, Integer maxNumberOfMessages, QueueAttributeName... attributeNames) {
        Assert.hasText(queueUrl, "参数 queueUrl 值不能为空,请检查");
        ReceiveMessageRequest.Builder requestBuilder = ReceiveMessageRequest.builder().queueUrl(queueUrl);
        if (maxNumberOfMessages != null && maxNumberOfMessages > 1 && maxNumberOfMessages < 10) {
            requestBuilder.maxNumberOfMessages(maxNumberOfMessages);
        }
        if (attributeNames != null && attributeNames.length > 0) {
            requestBuilder.attributeNames(attributeNames);
        }
        return SqsEndpointFactory.getSqsClient(clientName).receiveMessage(requestBuilder.build());
    }

    /**
     * 删除sqs 消息，删除成功则返回true
     *
     * @param clientName    连接名称 , 为空则使用 defaults
     * @param queueUrl      队列url地址
     * @param receiptHandle 凭证、收据
     * @return boolean
     * @author wuqiong 2022/6/25 15:25
     */
    public static boolean deleteMessage(String clientName, String queueUrl, String receiptHandle) {
        Assert.hasText(queueUrl, "参数 queueUrl 值不能为空,请检查");
        Assert.hasText(receiptHandle, "参数 receiptHandle 值不能为空,请检查");
        DeleteMessageRequest buildDeleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        DeleteMessageResponse deleteMessageResponse = SqsEndpointFactory.getSqsClient(clientName).deleteMessage(buildDeleteRequest);
        return deleteMessageResponse.sdkHttpResponse().isSuccessful();
    }

    /**
     * 批量删除消息
     *
     * @param clientName 连接名称 , 为空则使用 defaults
     * @param queueUrl   队列url地址
     * @param entries    需要删除的集合
     * @return DeleteMessageBatchResponses
     * @author wuqiong 2022/6/25 15:29
     */
    public static DeleteMessageBatchResponse deleteBatchMessage(String clientName, String queueUrl, DeleteMessageBatchRequestEntry... entries) {
        Assert.hasText(queueUrl, "参数 queueUrl 值不能为空,请检查");
        Assert.isTrue(entries == null || entries.length == 0, "参数 entries 数组值不能为空,请检查");
        DeleteMessageBatchRequest buildDeleteBatchRequest = DeleteMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build();
        return SqsEndpointFactory.getSqsClient(clientName).deleteMessageBatch(buildDeleteBatchRequest);
    }

    /**
     * 根据名称查询 queue url
     *
     * @param queueName  队列名称
     * @param clientName 连接名称 , 为空则使用 defaults
     * @return String
     * @author wuqiong 2022/8/5 14:01
     */
    public static String queryQueueUrl(String clientName, String queueName) {
        Assert.hasText(queueName, "参数 queueName 值不能为空,请检查");
        return SqsEndpointFactory.getSqsClient(clientName)
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build())
                .queueUrl();
    }

    private static SendMessageRequest.Builder createBuilder(String queueUrl, String messageBody, Integer delaySeconds, Map<String, MessageAttributeValue> messageAttributes) {
        Assert.hasText(queueUrl, "参数 queueUrl 值不能为空,请检查");
        Assert.hasText(messageBody, "参数 messageBody 值不能为空,请检查");
        SendMessageRequest.Builder builder = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody);
        if (delaySeconds != null) {
            Assert.isTrue(delaySeconds < 0 || delaySeconds > 900, "参数 delaySeconds的值介于 0-900之间 ,请检查");
            builder.delaySeconds(delaySeconds);
        }
        if (messageAttributes != null && !messageAttributes.isEmpty()) builder.messageAttributes(messageAttributes);
        return builder;
    }

}
