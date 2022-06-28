package com.duxinglangzi.sqs.starter.enums;


import com.duxinglangzi.sqs.starter.container.QueueMessageAcknowledgment;

/**
 * 消息删除策略
 *
 * @author wuqiong 2022/6/25
 */
public enum MessageDeletionPolicy {

    /**
     * 总是删除消息，无论消息消费成功或者失败
     */
    ALWAYS,

    /**
     * 从不自动删除消息。接收监听的方法必须使用 QueueMessageAcknowledgment 参数手动确认每条消息。
     * <p>
     * 提示：使用此策略时，监听方法必须注意删除消息。否则，它将导致消息无限循环。
     *
     * @see QueueMessageAcknowledgment
     */
    NEVER,


    /**
     * 监听的方法成功执行时删除消息。如果监听方法引发异常，则不会删除该消息。
     * <p>
     * 提示: 需要默认,为此设置死信队列. 否则将会引起消息无限循环
     */
    SUCCESS

}
