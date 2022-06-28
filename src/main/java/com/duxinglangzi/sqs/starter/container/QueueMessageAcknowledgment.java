package com.duxinglangzi.sqs.starter.container;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

/**
 * 队列消息确认
 *
 * @author wuqiong 2022/6/25
 */
public class QueueMessageAcknowledgment {

    private String queueUrl;
    private String receiptHandle;
    private SqsClient sqsClient;

    public QueueMessageAcknowledgment(String queueUrl, String receiptHandle, SqsClient sqsClient) {
        this.queueUrl = queueUrl;
        this.receiptHandle = receiptHandle;
        this.sqsClient = sqsClient;
    }


    /**
     * 确认已收到消息后,执行删除动作
     *
     * @return boolean
     * @author wuqiong 2022-06-25 17:08
     */
    public boolean acknowledge() {
        DeleteMessageRequest buildDeleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        return sqsClient.deleteMessage(buildDeleteRequest).sdkHttpResponse().isSuccessful();
    }

}
