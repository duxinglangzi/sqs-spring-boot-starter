package com.duxinglangzi.sqs.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.regions.Region;

import java.util.Map;

/**
 * sqs 配置信息
 * <p>
 * 在 yml 配置方式如下:
 * <pre>
 * duxinglangzi:
 *   queue:
 *     sqs:
 *       instances:
 *         defaults: # 默认的，如果不设置，程序会将第一个设置为默认的
 *           region: us-west-1
 *           secret-access-key: Ya8raaaaaaaaaaaaaaaaaaaad8AjBii29x
 *           access-key-id: AKbbbHbbbbbbbUF27W
 *         second:  # 第二个
 *           region: us-west-2
 *           secret-access-key: Ya8rccccccccccccccccccccd8AjBii29x
 *           access-key-id: AKIddddddddd7UF27W
 *         third:
 *           region: us-east-1
 *           secret-access-key: Ya8reeeeeeeeeeeeeeeeeeeeeAjBii29x
 *           access-key-id: AKfffffffffffUF27W
 *
 * </pre>
 *
 * @author wuqiong 2022/6/25
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConfigurationProperties(prefix = "duxinglangzi.queue.sqs")
public class SqsConfig {

    private Map<String, EndpointInstance> instances;

    public static class EndpointInstance {
        private String accessKeyId;
        private String secretAccessKey;
        /**
         * 地区
         * <p> 参考 {@link Region}
         */
        private String region;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public Map<String, EndpointInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, EndpointInstance> instances) {
        this.instances = instances;
    }
}
