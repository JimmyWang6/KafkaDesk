package com.kafkadesk.core.service;

import com.kafkadesk.model.Message;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息消费服务
 */
public class ConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);
    private static ConsumerService instance;
    private final Map<String, KafkaConsumer<String, String>> consumers = new ConcurrentHashMap<>();

    private ConsumerService() {
    }

    public static synchronized ConsumerService getInstance() {
        if (instance == null) {
            instance = new ConsumerService();
        }
        return instance;
    }

    /**
     * 创建消费者
     */
    public KafkaConsumer<String, String> createConsumer(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        String consumerId = bootstrapServers + "_" + groupId + "_" + UUID.randomUUID().toString();
        consumers.put(consumerId, consumer);
        
        return consumer;
    }

    /**
     * 订阅主题
     */
    public void subscribe(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        logger.info("Subscribed to topic: {}", topic);
    }

    /**
     * 订阅指定分区
     */
    public void assignPartitions(KafkaConsumer<String, String> consumer, String topic, List<Integer> partitions) {
        List<TopicPartition> topicPartitions = new ArrayList<>();
        for (Integer partition : partitions) {
            topicPartitions.add(new TopicPartition(topic, partition));
        }
        consumer.assign(topicPartitions);
        logger.info("Assigned partitions: {}", topicPartitions);
    }

    /**
     * 从最早位置开始消费
     */
    public void seekToBeginning(KafkaConsumer<String, String> consumer) {
        Set<TopicPartition> assignments = consumer.assignment();
        if (!assignments.isEmpty()) {
            consumer.seekToBeginning(assignments);
            logger.info("Seek to beginning for partitions: {}", assignments);
        }
    }

    /**
     * 从最新位置开始消费
     */
    public void seekToEnd(KafkaConsumer<String, String> consumer) {
        Set<TopicPartition> assignments = consumer.assignment();
        if (!assignments.isEmpty()) {
            consumer.seekToEnd(assignments);
            logger.info("Seek to end for partitions: {}", assignments);
        }
    }

    /**
     * 从指定 offset 开始消费
     */
    public void seek(KafkaConsumer<String, String> consumer, String topic, int partition, long offset) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.seek(topicPartition, offset);
        logger.info("Seek to offset {} for partition: {}", offset, topicPartition);
    }

    /**
     * 轮询消息
     */
    public List<Message> poll(KafkaConsumer<String, String> consumer, long timeoutMs) {
        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutMs));
            List<Message> messages = new ArrayList<>();

            for (ConsumerRecord<String, String> record : records) {
                Message message = convertToMessage(record);
                messages.add(message);
            }

            return messages;
        } catch (Exception e) {
            logger.error("Error polling messages", e);
            return Collections.emptyList();
        }
    }

    /**
     * 转换消息
     */
    private Message convertToMessage(ConsumerRecord<String, String> record) {
        Message message = new Message();
        message.setTopic(record.topic());
        message.setPartition(record.partition());
        message.setOffset(record.offset());
        message.setKey(record.key());
        message.setValue(record.value());
        message.setTimestamp(record.timestamp());

        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        message.setHeaders(headers);

        return message;
    }

    /**
     * 提交 offset
     */
    public void commitSync(KafkaConsumer<String, String> consumer) {
        try {
            consumer.commitSync();
            logger.info("Offset committed successfully");
        } catch (Exception e) {
            logger.error("Failed to commit offset", e);
        }
    }

    /**
     * 关闭消费者
     */
    public void closeConsumer(KafkaConsumer<String, String> consumer) {
        if (consumer != null) {
            try {
                consumer.close();
                logger.info("Consumer closed");
            } catch (Exception e) {
                logger.error("Error closing consumer", e);
            }
        }
    }

    /**
     * 关闭所有消费者
     */
    public void closeAllConsumers() {
        consumers.values().forEach(consumer -> {
            try {
                consumer.close();
            } catch (Exception e) {
                logger.error("Error closing consumer", e);
            }
        });
        consumers.clear();
    }
}
