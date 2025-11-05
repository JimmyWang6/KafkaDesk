package com.kafkadesk.service;

import com.kafkadesk.model.TopicInfo;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for managing Kafka topics
 */
public class TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    private final KafkaConnectionService connectionService;

    public TopicService(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * List all topics in a cluster
     */
    public List<TopicInfo> listTopics(String connectionId) throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ListTopicsResult listTopicsResult = adminClient.listTopics();
        Set<String> topicNames = listTopicsResult.names().get();

        DescribeTopicsResult describeResult = adminClient.describeTopics(topicNames);
        Map<String, TopicDescription> descriptions = describeResult.all().get();

        List<TopicInfo> topics = new ArrayList<>();
        for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
            TopicDescription desc = entry.getValue();
            TopicInfo info = new TopicInfo();
            info.setName(desc.name());
            info.setPartitions(desc.partitions().size());
            
            if (!desc.partitions().isEmpty()) {
                info.setReplicationFactor((short) desc.partitions().get(0).replicas().size());
            }
            
            topics.add(info);
        }

        logger.info("Listed {} topics", topics.size());
        return topics;
    }

    /**
     * Create a new topic
     */
    public void createTopic(String connectionId, String topicName, int partitions, short replicationFactor) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
        CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
        result.all().get();
        
        logger.info("Created topic: {} with {} partitions and replication factor {}", 
                   topicName, partitions, replicationFactor);
    }

    /**
     * Delete a topic
     */
    public void deleteTopic(String connectionId, String topicName) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
        result.all().get();
        
        logger.info("Deleted topic: {}", topicName);
    }

    /**
     * Get topic configuration
     */
    public Map<String, String> getTopicConfig(String connectionId, String topicName) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singleton(resource));
        Config config = result.all().get().get(resource);

        return config.entries().stream()
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
    }

    /**
     * Update topic configuration
     */
    public void updateTopicConfig(String connectionId, String topicName, Map<String, String> configs) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        Collection<AlterConfigOp> ops = configs.entrySet().stream()
                .map(entry -> new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()),
                        AlterConfigOp.OpType.SET))
                .collect(Collectors.toList());

        Map<ConfigResource, Collection<AlterConfigOp>> alterConfigs = new HashMap<>();
        alterConfigs.put(resource, ops);

        AlterConfigsResult result = adminClient.incrementalAlterConfigs(alterConfigs);
        result.all().get();
        
        logger.info("Updated configuration for topic: {}", topicName);
    }

    /**
     * Get topic details
     */
    public TopicInfo getTopicDetails(String connectionId, String topicName) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeTopicsResult result = adminClient.describeTopics(Collections.singleton(topicName));
        TopicDescription description = result.all().get().get(topicName);

        TopicInfo info = new TopicInfo();
        info.setName(description.name());
        info.setPartitions(description.partitions().size());
        
        if (!description.partitions().isEmpty()) {
            info.setReplicationFactor((short) description.partitions().get(0).replicas().size());
        }

        return info;
    }
}
