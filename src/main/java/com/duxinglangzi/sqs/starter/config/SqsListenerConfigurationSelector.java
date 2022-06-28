package com.duxinglangzi.sqs.starter.config;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author wuqiong 2022/6/25
 */
public class SqsListenerConfigurationSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{SqsBootstrapConfiguration.class.getName()};
    }
}
