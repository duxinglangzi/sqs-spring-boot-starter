package com.duxinglangzi.sqs.starter.container;

import com.duxinglangzi.sqs.starter.common.Constants;
import com.duxinglangzi.sqs.starter.enums.MessageDeletionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息拉取的容器
 *
 * @author wuqiong 2022/6/25
 */
public class MessageListenerContainer extends AbstractMessageListenerContainer {
    private static final Logger logger = LoggerFactory.getLogger(MessageListenerContainer.class);

    private String queueUrl;
    private String queueName;
    private Method method;
    private Object bean;
    private SqsClient sqsClient;
    private MessageDeletionPolicy deletionPolicy;
    private List<QueueAttributeName> attributeNames;
    private List<String> messageAttributeNames;
    private AsyncTaskExecutor asyncTaskExecutor;
    private boolean isFifoQueue = false;
    private ReceiveMessageRequest buildRequest;

    public MessageListenerContainer(
            String queueName, List<QueueAttributeName> attributeNames, List<String> messageAttributeNames,
            MessageDeletionPolicy deletionPolicy, Method method, Object bean, SqsClient sqsClient, AsyncTaskExecutor asyncTaskExecutor) {
        this.queueName = queueName;
        this.attributeNames = attributeNames;
        this.messageAttributeNames = messageAttributeNames;
        this.deletionPolicy = deletionPolicy;
        this.method = method;
        this.bean = bean;
        this.sqsClient = sqsClient;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    public void doInit() {
        queueUrl = resolveDestination(queueName);
        isFifoQueue = checkFifoQueue(queueUrl);
        ReceiveMessageRequest.Builder requestBuilder = ReceiveMessageRequest.builder().queueUrl(queueUrl);
        requestBuilder.maxNumberOfMessages(Constants.DEFAULT_BATCH_MESSAGE);// 最大10条消息
        requestBuilder.waitTimeSeconds(Constants.DEFAULT_WAIT_TIME_SECONDS);// 长轮询10秒
        if (attributeNames != null) requestBuilder.attributeNames(attributeNames);
        if (messageAttributeNames != null) requestBuilder.messageAttributeNames(messageAttributeNames);
        this.buildRequest = requestBuilder.build();
    }

    public void doStart() {
        try {
            ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(this.buildRequest);
            if (!receiveMessageResponse.hasMessages()) {
                sleep(SLEEP_TIME_MILLI_SECONDS);
                if (!isRunning()) Thread.currentThread().interrupt();
                return;
            }
            if (isFifoQueue) {
                for (Message message : receiveMessageResponse.messages()) {
                    Exception exception = null;
                    try {
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
                CountDownLatch countDownLatch = new CountDownLatch(receiveMessageResponse.messages().size());
                Map<String, String> concurrentHashMap = new ConcurrentHashMap();
                for (Message message : receiveMessageResponse.messages()) {
                    this.asyncTaskExecutor.execute(new SignalExecutingRunnable(countDownLatch, new MessageExecutor(message, concurrentHashMap)));
                }
                try {
                    countDownLatch.await();
                    if (!concurrentHashMap.isEmpty()) deleteBatchMessage(concurrentHashMap);
                } catch (InterruptedException e) {
                    logger.error("[MessageListenerContainer_doStart_countDownLatch] 消息消费过程中多线程发生异常, queueUrl:{} ,deletionPolicy:{}",
                            queueUrl, deletionPolicy.name());
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception exc) {
            logger.error("[MessageListenerContainer_doStart] 拉取消息发生异常 ,Pull message exception. queueUrl:{} ,methodName:{} ,errorMessage:{}",
                    queueUrl, method.getName(), exc.getLocalizedMessage());
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

    private boolean checkFifoQueue(String queueUrl) {
        GetQueueAttributesResponse queueAttributes = this.sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder().queueUrl(queueUrl).attributeNames(QueueAttributeName.ALL).build());
        String fifoQueue = queueAttributes.attributes().get(QueueAttributeName.FIFO_QUEUE);
        return Boolean.TRUE.toString().equals(fifoQueue);
    }

    private boolean deleteBatchMessage(Map<String, String> concurrentHashMap) {
        List<DeleteMessageBatchRequestEntry> entries = concurrentHashMap.entrySet().stream().map(e -> DeleteMessageBatchRequestEntry.builder().id(e.getKey()).receiptHandle(e.getValue()).build()).collect(Collectors.toList());
        DeleteMessageBatchRequest build =
                DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build();
        boolean isDelete = sqsClient.deleteMessageBatch(build).sdkHttpResponse().isSuccessful();
        if (!isDelete)
            throw new RuntimeException("Message batch Cannot delete ,receiptHandleMap: " + concurrentHashMap.toString());
        return true;
    }

    private String resolveDestination(String queueName) {
        if (isValidQueueUrl(queueName)) {
            return queueName;
        }
        return this.sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }

    private static boolean isValidQueueUrl(String name) {
        try {
            URI candidate = new URI(name);
            return ("http".equals(candidate.getScheme()) || "https".equals(candidate.getScheme()));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private class MessageExecutor implements Runnable {
        private final Message message;
        private final Map<String, String> concurrentHashMap;

        private MessageExecutor(Message message, Map<String, String> concurrentHashMap) {
            this.message = message;
            this.concurrentHashMap = concurrentHashMap;
        }

        @Override
        public void run() {
            Exception exception = null;
            try {
                if (MessageDeletionPolicy.NEVER == deletionPolicy) {
                    method.invoke(bean, this.message, createAck(message));
                } else {
                    method.invoke(bean, this.message);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                exception = e;
                e.printStackTrace();
            }
            switch (deletionPolicy) {
                case ALWAYS:
                    concurrentHashMap.put(this.message.messageId(), this.message.receiptHandle());
                case SUCCESS:
                    if (exception == null)
                        concurrentHashMap.put(this.message.messageId(), this.message.receiptHandle());
            }
        }
    }

    private static class SignalExecutingRunnable implements Runnable {
        private final CountDownLatch countDownLatch;
        private final Runnable runnable;

        private SignalExecutingRunnable(CountDownLatch endSignal, Runnable runnable) {
            this.countDownLatch = endSignal;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                this.runnable.run();
            } finally {
                this.countDownLatch.countDown();
            }
        }
    }

}
