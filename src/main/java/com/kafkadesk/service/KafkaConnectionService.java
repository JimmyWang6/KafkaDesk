package com.kafkadesk.service;

import com.kafkadesk.model.KafkaConnection;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Kafka connections
 */
public class KafkaConnectionService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConnectionService.class);
    
    private final ConcurrentHashMap<String, AdminClient> adminClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

    /**
     * Test connection to Kafka cluster
     */
    public boolean testConnection(KafkaConnection connection) {
        AdminClient adminClient = null;
        try {
            Properties props = createAdminProperties(connection);
            adminClient = AdminClient.create(props);
            
            // Try to list topics as a connection test
            adminClient.listTopics().names().get();
            logger.info("Connection test successful for: {}", connection.getName());
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed for: {}", connection.getName(), e);
            return false;
        } finally {
            if (adminClient != null) {
                adminClient.close();
            }
        }
    }

    /**
     * Connect to Kafka cluster and cache the admin client
     */
    public void connect(KafkaConnection connection) throws Exception {
        if (adminClients.containsKey(connection.getId())) {
            logger.info("Already connected to: {}", connection.getName());
            return;
        }

        Properties props = createAdminProperties(connection);
        AdminClient adminClient = AdminClient.create(props);
        
        // Test the connection
        adminClient.listTopics().names().get();
        
        adminClients.put(connection.getId(), adminClient);
        logger.info("Successfully connected to: {}", connection.getName());
    }

    /**
     * Disconnect from Kafka cluster
     */
    public void disconnect(String connectionId) {
        AdminClient adminClient = adminClients.remove(connectionId);
        if (adminClient != null) {
            adminClient.close();
            logger.info("Disconnected from connection: {}", connectionId);
        }

        KafkaProducer<String, String> producer = producers.remove(connectionId);
        if (producer != null) {
            producer.close();
        }
    }

    /**
     * Get AdminClient for a connection
     */
    public AdminClient getAdminClient(String connectionId) {
        return adminClients.get(connectionId);
    }

    /**
     * Get or create KafkaProducer for a connection
     */
    public KafkaProducer<String, String> getProducer(KafkaConnection connection) {
        return producers.computeIfAbsent(connection.getId(), id -> {
            Properties props = createProducerProperties(connection);
            return new KafkaProducer<>(props);
        });
    }

    /**
     * Create KafkaConsumer for a connection
     */
    public KafkaConsumer<String, String> createConsumer(KafkaConnection connection) {
        Properties props = createConsumerProperties(connection);
        return new KafkaConsumer<>(props);
    }

    /**
     * Check if connected to a cluster
     */
    public boolean isConnected(String connectionId) {
        return adminClients.containsKey(connectionId);
    }

    /**
     * Close all connections
     */
    public void closeAll() {
        adminClients.values().forEach(AdminClient::close);
        adminClients.clear();
        producers.values().forEach(KafkaProducer::close);
        producers.clear();
    }

    private Properties createAdminProperties(KafkaConnection connection) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

        if (connection.isUseSasl()) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", connection.getSaslMechanism());
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                connection.getSaslUsername(),
                connection.getSaslPassword()
            );
            props.put("sasl.jaas.config", jaasConfig);
        }

        return props;
    }

    private Properties createProducerProperties(KafkaConnection connection) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);

        if (connection.isUseSasl()) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", connection.getSaslMechanism());
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                connection.getSaslUsername(),
                connection.getSaslPassword()
            );
            props.put("sasl.jaas.config", jaasConfig);
        }

        return props;
    }

    private Properties createConsumerProperties(KafkaConnection connection) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafkadesk-" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        if (connection.isUseSasl()) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", connection.getSaslMechanism());
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                connection.getSaslUsername(),
                connection.getSaslPassword()
            );
            props.put("sasl.jaas.config", jaasConfig);
        }

        return props;
    }
}
