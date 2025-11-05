package com.kafkadesk.service;

import com.kafkadesk.model.KafkaConnection;
import com.kafkadesk.model.KafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Service for sending and browsing Kafka messages
 */
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final KafkaConnectionService connectionService;

    public MessageService(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Send a message to a topic
     */
    public RecordMetadata sendMessage(KafkaConnection connection, String topic, String key, String value) 
            throws Exception {
        KafkaProducer<String, String> producer = connectionService.getProducer(connection);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        Future<RecordMetadata> future = producer.send(record);
        RecordMetadata metadata = future.get();
        
        logger.info("Sent message to topic: {}, partition: {}, offset: {}", 
                   topic, metadata.partition(), metadata.offset());
        
        return metadata;
    }

    /**
     * Send a message with headers to a topic
     */
    public RecordMetadata sendMessage(KafkaConnection connection, String topic, String key, 
                                     String value, Map<String, String> headers) throws Exception {
        KafkaProducer<String, String> producer = connectionService.getProducer(connection);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        
        if (headers != null) {
            headers.forEach((k, v) -> record.headers().add(k, v.getBytes()));
        }
        
        Future<RecordMetadata> future = producer.send(record);
        RecordMetadata metadata = future.get();
        
        logger.info("Sent message with headers to topic: {}, partition: {}, offset: {}", 
                   topic, metadata.partition(), metadata.offset());
        
        return metadata;
    }

    /**
     * Browse messages from a topic (latest N messages)
     */
    public List<KafkaMessage> browseMessages(KafkaConnection connection, String topic, int maxMessages) {
        KafkaConsumer<String, String> consumer = null;
        try {
            consumer = connectionService.createConsumer(connection);
            
            // Get all partitions for the topic
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());
            
            consumer.assign(partitions);
            
            // Seek to the end and then seek back
            consumer.seekToEnd(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            
            for (TopicPartition partition : partitions) {
                long endOffset = endOffsets.get(partition);
                long startOffset = Math.max(0, endOffset - maxMessages);
                consumer.seek(partition, startOffset);
            }
            
            List<KafkaMessage> messages = new ArrayList<>();
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                if (messages.size() >= maxMessages) {
                    break;
                }
                messages.add(convertToKafkaMessage(record));
            }
            
            logger.info("Browsed {} messages from topic: {}", messages.size(), topic);
            return messages;
            
        } catch (Exception e) {
            logger.error("Error browsing messages from topic: {}", topic, e);
            return Collections.emptyList();
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    /**
     * Browse messages from a specific partition
     */
    public List<KafkaMessage> browseMessagesFromPartition(KafkaConnection connection, String topic, 
                                                          int partition, long startOffset, int maxMessages) {
        KafkaConsumer<String, String> consumer = null;
        try {
            consumer = connectionService.createConsumer(connection);
            
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            consumer.assign(Collections.singletonList(topicPartition));
            consumer.seek(topicPartition, startOffset);
            
            List<KafkaMessage> messages = new ArrayList<>();
            int pollAttempts = 0;
            int maxPollAttempts = 10;
            
            while (messages.size() < maxMessages && pollAttempts < maxPollAttempts) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                
                for (ConsumerRecord<String, String> record : records) {
                    if (messages.size() >= maxMessages) {
                        break;
                    }
                    messages.add(convertToKafkaMessage(record));
                }
                
                if (records.isEmpty()) {
                    pollAttempts++;
                } else {
                    pollAttempts = 0;
                }
            }
            
            logger.info("Browsed {} messages from topic: {}, partition: {}", messages.size(), topic, partition);
            return messages;
            
        } catch (Exception e) {
            logger.error("Error browsing messages from topic: {}, partition: {}", topic, partition, e);
            return Collections.emptyList();
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    /**
     * Search messages by key or value pattern
     */
    public List<KafkaMessage> searchMessages(KafkaConnection connection, String topic, 
                                            String searchTerm, int maxMessages) {
        KafkaConsumer<String, String> consumer = null;
        try {
            consumer = connectionService.createConsumer(connection);
            
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());
            
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            
            List<KafkaMessage> matchedMessages = new ArrayList<>();
            int pollAttempts = 0;
            int maxPollAttempts = 20;
            
            while (matchedMessages.size() < maxMessages && pollAttempts < maxPollAttempts) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                
                for (ConsumerRecord<String, String> record : records) {
                    if (matchedMessages.size() >= maxMessages) {
                        break;
                    }
                    
                    boolean matches = (record.key() != null && record.key().contains(searchTerm)) ||
                                     (record.value() != null && record.value().contains(searchTerm));
                    
                    if (matches) {
                        matchedMessages.add(convertToKafkaMessage(record));
                    }
                }
                
                if (records.isEmpty()) {
                    pollAttempts++;
                } else {
                    pollAttempts = 0;
                }
            }
            
            logger.info("Found {} matching messages in topic: {}", matchedMessages.size(), topic);
            return matchedMessages;
            
        } catch (Exception e) {
            logger.error("Error searching messages in topic: {}", topic, e);
            return Collections.emptyList();
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private KafkaMessage convertToKafkaMessage(ConsumerRecord<String, String> record) {
        KafkaMessage message = new KafkaMessage();
        message.setTopic(record.topic());
        message.setPartition(record.partition());
        message.setOffset(record.offset());
        message.setTimestamp(Instant.ofEpochMilli(record.timestamp()));
        message.setKey(record.key());
        message.setValue(record.value());
        
        // Convert headers to string
        if (record.headers() != null) {
            StringBuilder headersStr = new StringBuilder();
            for (Header header : record.headers()) {
                if (headersStr.length() > 0) {
                    headersStr.append(", ");
                }
                headersStr.append(header.key()).append("=").append(new String(header.value()));
            }
            message.setHeaders(headersStr.toString());
        }
        
        return message;
    }
}
