# sqs-spring-boot-starter
# Aws Sqs Spring Boot Starter 使用实例

### 在spring boot 项目配置文件 application.yml内增加以下内容
```yaml
duxinglangzi:
  queue:
    sqs:
      instances:
        defaults:               # 默认的，如果不设置，程序会将第一个设置为默认的
          region: us-west-1
          secret-access-key: Ya8raaaaaaaaaaaaaaaaaaaad8AjBii29x
          access-key-id: AKbbbHbbbbbbbUF27W
        second:                 # 第二个
          region: us-west-2
          secret-access-key: Ya8rccccccccccccccccccccd8AjBii29x
          access-key-id: AKIddddddddd7UF27W
        third:                  # 第三个
          region: us-east-1
          secret-access-key: Ya8reeeeeeeeeeeeeeeeeeeeeAjBii29x
          access-key-id: AKfffffffffffUF27W
wuqiong:
  sqs:
    url: https://sqs.us-west-1.amazonaws.com/1234567890/qiong-queue.fifo

```

### 在spring boot 项目中的代码使用实例 (需在 使用EnableSqsListener 注解开启 sqs listener)

```java


import com.duxinglangzi.sqs.starter.annotation.EnableSqsListener;
import com.duxinglangzi.sqs.starter.annotation.SqsListener;
import com.duxinglangzi.sqs.starter.container.QueueMessageAcknowledgment;
import com.duxinglangzi.sqs.starter.enums.MessageDeletionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import org.springframework.stereotype.Service;


@Service
@EnableSqsListener
public class SqsListenerTest {

    /**
     * 必须添加 @EnableSqsListener 注解,才能能用
     */
    @Autowired
    private CustomSqsClient customSqsClient;

    /**
     * 示例1:  通过动态参数进行配置 listener, 且删除策略为: NEVER(手动确认并删除)
     *
     * @param message
     * @param acknowledgment
     * @return void
     * @author wuqiong 2022-06-28 17:28
     */
    @SqsListener(queueUrl = "${wuqiong.sqs.url}", deletionPolicy = MessageDeletionPolicy.NEVER)
    public void oneMessage(Message message, QueueMessageAcknowledgment acknowledgment) {
        System.out.println("[oneMessage] --->>> currentTimeMillis ： " +
                System.currentTimeMillis() + "Fifo message body " + message.body());
        boolean acknowledge = acknowledgment.acknowledge(); // 手动删除消息
    }

    /**
     * 示例2:  通过指定队列地址, 且删除策略为默认的: SUCCESS , 此时方法无需参数 QueueMessageAcknowledgment
     *
     * @param message
     * @return void
     * @author wuqiong 2022-06-28 17:28
     */
    @SqsListener(queueUrl = "https://sqs.us-west-1.amazonaws.com/1234567890/qiong-queue.fifo")
    public void twoMessage(Message message) {
        System.out.println("[twoMessage] --->>> currentTimeMillis ： " +
                System.currentTimeMillis() + "Fifo message body " + message.body());
    }

    /**
     * 示例3:  通过指定队列地址, 且删除策略为: ALWAYS(总是删除消息) , 此时方法无需参数 QueueMessageAcknowledgment
     *
     * @param message
     * @return void
     * @author wuqiong 2022-06-28 17:28
     */
    @SqsListener(queueUrl = "https://sqs.us-west-1.amazonaws.com/1234567890/qiong-queue.fifo", deletionPolicy = MessageDeletionPolicy.ALWAYS)
    public void threeMessage(Message message) {
        System.out.println("[threeMessage] --->>> currentTimeMillis ： " +
                System.currentTimeMillis() + "Fifo message body " + message.body());
    }

    /**
     * 示例4:  发送一个消息到 sqs fifo 队列
     *
     * @return void
     * @author wuqiong 2022-06-28 17:28
     */
    public void fourMessage() {
        // second 对应着配置文件第二个
        customSqsClient.sentFifoMessage(
                "second",
                "https://sqs.us-west-1.amazonaws.com/1234567890/qiong-queue.fifo",
                "这是测试消息啊",
                "test-group-11111",
                "test-group",
                3,
                null);
    }

    /**
     * 示例5:  发送一个消息到 sqs standard 队列
     *
     * @return void
     * @author wuqiong 2022-06-28 17:28
     */
    public void fiveMessage() {
        // client name 不写, 默认使用配置文件里面的第一个
        SendMessageResponse sendMessageResponse = customSqsClient.sentStandardMessage(
                null,
                "https://sqs.us-west-1.amazonaws.com/1234567890/qiong-standard-queue",
                "这是测试消息啊",
                3,
                null);
        System.out.println(sendMessageResponse.messageId());
    }

}



```




