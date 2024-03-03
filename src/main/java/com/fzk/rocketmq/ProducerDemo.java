package com.fzk.rocketmq;

import com.fzk.log.Logger;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * rocketmq 生产者
 *
 * @author zhike.feng
 * @datetime 2023-11-26 22:28:47
 */
public class ProducerDemo implements AutoCloseable {
    public static void main(String[] args) {
        try (ProducerDemo producer = new ProducerDemo()) {
            producer.sendMsg("fzk", "test_tag", "hello");
        }
    }

    private final Producer producer;
    private final ClientServiceProvider provider;
    private final String topic = "test_topic";

    public void sendMsg(String key, String tag, String content) {
        try {
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    // 设置消息索引键，可根据关键字精确查找某条消息。
                    .setKeys(key)
                    // 设置消息Tag，用于消费端根据指定Tag过滤消息。
                    .setTag(tag)
                    // 消息体。
                    .setBody(content.getBytes(StandardCharsets.UTF_8))
                    .build();
            // 发送消息，需要关注发送结果，并捕获失败等异常。
            SendReceipt sendReceipt = producer.send(message);
            Logger.info(String.format("Send message successfully, messageId={%s}", sendReceipt.getMessageId()));
        } catch (ClientException e) {
            Logger.error(String.format("Failed to send message, err: %s", e.toString()));
        }
    }

    public ProducerDemo() {
        this.provider = ClientServiceProvider.loadService();
        // 接入点地址，需要设置成Proxy的地址和端口列表，一般是xxx:8081;xxx:8081。
        String endpoint = "124.223.192.8:8081";
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoint).enableSsl(false);
        ClientConfiguration configuration = builder.build();
        try {
            // 初始化Producer时需要设置通信配置以及预绑定的Topic。
            this.producer = provider.newProducerBuilder()
                    .setTopics(topic)
                    .setClientConfiguration(configuration)
                    .build();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void close() {
        try {
            this.producer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
