package com.kafkadesk.core.service;

import com.kafkadesk.model.Message;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Message producer service
 */
public class ProducerService {
    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);
    private static ProducerService instance;
    private final Map<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

    private ProducerService() {
    }

    public static synchronized ProducerService getInstance() {
        if (instance == null) {
            instance = new ProducerService();
        }
        return instance;
    }

    /**
     * Get or create Producer
     */
    private KafkaProducer<String, String> getProducer(String bootstrapServers) {
        return producers.computeIfAbsent(bootstrapServers, servers -> {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);

            return new KafkaProducer<>(props);
        });
    }

    /**
     * Send message
     */
    public RecordMetadata sendMessage(String bootstrapServers, Message message) throws Exception {
        KafkaProducer<String, String> producer = getProducer(bootstrapServers);

        List<Header> headers = new ArrayList<>();
        if (message.getHeaders() != null) {
            message.getHeaders().forEach((key, value) -> 
                headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)))
            );
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(
                message.getTopic(),
                message.getPartition(),
                message.getKey(),
                message.getValue(),
                headers
        );

        try {
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            logger.info("Message sent successfully to topic: {}, partition: {}, offset: {}", 
                    metadata.topic(), metadata.partition(), metadata.offset());
            return metadata;
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            throw e;
        }
    }

    /**
     * Send message（With callback）
     */
    public void sendMessage(String bootstrapServers, Message message, Callback callback) {
        KafkaProducer<String, String> producer = getProducer(bootstrapServers);

        List<Header> headers = new ArrayList<>();
        if (message.getHeaders() != null) {
            message.getHeaders().forEach((key, value) -> 
                headers.add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)))
            );
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(
                message.getTopic(),
                message.getPartition(),
                message.getKey(),
                message.getValue(),
                headers
        );

        producer.send(record, callback);
    }

    /**
     * 批量Send message
     */
    public List<RecordMetadata> sendMessages(String bootstrapServers, List<Message> messages) throws Exception {
        List<RecordMetadata> metadataList = new ArrayList<>();
        for (Message message : messages) {
            RecordMetadata metadata = sendMessage(bootstrapServers, message);
            metadataList.add(metadata);
        }
        return metadataList;
    }

    /**
     * Close producer
     */
    public void closeProducer(String bootstrapServers) {
        KafkaProducer<String, String> producer = producers.remove(bootstrapServers);
        if (producer != null) {
            try {
                producer.close();
                logger.info("Producer closed for: {}", bootstrapServers);
            } catch (Exception e) {
                logger.error("Error closing producer", e);
            }
        }
    }

    /**
     * Close all producers
     */
    public void closeAllProducers() {
        producers.forEach((servers, producer) -> {
            try {
                producer.close();
            } catch (Exception e) {
                logger.error("Error closing producer for: " + servers, e);
            }
        });
        producers.clear();
    }
}
