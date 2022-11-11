package com.duxinglangzi.sqs.starter.annotation;

import com.duxinglangzi.sqs.starter.config.SqsConfig;
import com.duxinglangzi.sqs.starter.config.SqsListenerConfigurationSelector;
import com.duxinglangzi.sqs.starter.listener.ApplicationReadyListener;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启 sqs listener
 *
 * @author wuqiong 2022/8/2
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SqsConfig.class, SqsListenerConfigurationSelector.class, ApplicationReadyListener.class})
public @interface EnableSqsListener {
}
