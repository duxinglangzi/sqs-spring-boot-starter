package com.duxinglangzi.sqs.starter.factory;

import com.duxinglangzi.sqs.starter.config.SqsConfig;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuqiong 2022/6/25
 */
public class SqsEndpointFactory {

    private static final Map<String, SqsClient> endpointMap = new ConcurrentHashMap<>();
    private static final String defaultStr = "defaults";

    /**
     * get sqs client endpoint
     *
     * @param clientName
     * @return SqsClient
     * @author wuqiong 2022/6/25 14:27
     */
    public static SqsClient getSqsClient(String clientName) {
        if (clientName == null || "".equals(clientName.trim())) clientName = defaultStr;
        return endpointMap.get(clientName);
    }

    /**
     * create batch sqs client
     *
     * @param sqsConfig
     * @author wuqiong 2022/6/25 14:13
     */
    public static void createBatchByConfig(SqsConfig sqsConfig) {
        Assert.isTrue(!sqsConfig.getInstances().isEmpty(), "Sqs config 为空,请检查");
        if (!sqsConfig.getInstances().containsKey(defaultStr)) {
            Optional<String> first = sqsConfig.getInstances().keySet().stream().findFirst();
            SqsClient sqsClient = createSqsClient(first.get(), sqsConfig.getInstances().get(first.get()));
            endpointMap.put(first.get(), sqsClient);
            endpointMap.put(defaultStr, sqsClient);
        }
        sqsConfig.getInstances().forEach((k, v) -> endpointMap.put(k,createSqsClient(k, v)));
    }

    /**
     * create sqs client by config endpointInstance
     *
     * @return SqsClient
     * @author wuqiong 2022/6/25 13:22
     */
    public static synchronized SqsClient createSqsClient(String clientName, SqsConfig.EndpointInstance endpointInstance) {
        Assert.isTrue(endpointInstance != null, "endpointInstance is null , please check ");
        if (endpointMap.containsKey(clientName)) return endpointMap.get(clientName);
        return SqsClient.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return new AwsCredentials() {
                            @Override
                            public String accessKeyId() {
                                return endpointInstance.getAccessKeyId();
                            }

                            @Override
                            public String secretAccessKey() {
                                return endpointInstance.getSecretAccessKey();
                            }
                        };
                    }
                })
                .region(Region.of(endpointInstance.getRegion()))
                .build();

    }
}
