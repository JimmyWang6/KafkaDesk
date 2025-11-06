package com.kafkadesk.core.service;

import com.kafkadesk.model.TopicInfo;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Topic service
 */
public class TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);
    private static TopicService instance;

    private TopicService() {
    }

    public static synchronized TopicService getInstance() {
        if (instance == null) {
            instance = new TopicService();
        }
        return instance;
    }

    /**
     * List all topics
     */
    public List<String> listTopics(String clusterId) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            logger.error("Admin client not found for cluster: {}", clusterId);
            return Collections.emptyList();
        }

        try {
            ListTopicsResult result = admin.listTopics();
            Set<String> topics = result.names().get(10, TimeUnit.SECONDS);
            return new ArrayList<>(topics);
        } catch (Exception e) {
            logger.error("Failed to list topics", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get topic details
     */
    public TopicInfo getTopicInfo(String clusterId, String topicName) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            return null;
        }

        try {
            DescribeTopicsResult result = admin.describeTopics(Collections.singleton(topicName));
            TopicDescription description = result.all().get(10, TimeUnit.SECONDS).get(topicName);

            if (description == null) {
                return null;
            }

            TopicInfo topicInfo = new TopicInfo();
            topicInfo.setName(description.name());
            topicInfo.setPartitions(description.partitions().size());

            // 获取副本因子（从第一个分区获取）
            if (!description.partitions().isEmpty()) {
                topicInfo.setReplicationFactor(description.partitions().get(0).replicas().size());
            }

            // Convert partition information
            List<TopicInfo.PartitionInfo> partitionInfos = description.partitions().stream()
                    .map(this::convertPartitionInfo)
                    .collect(Collectors.toList());
            topicInfo.setPartitionDetails(partitionInfos);

            // 获取主题配置
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            DescribeConfigsResult configResult = admin.describeConfigs(Collections.singleton(resource));
            Config config = configResult.all().get(10, TimeUnit.SECONDS).get(resource);

            if (config != null) {
                Map<String, String> configMap = new HashMap<>();
                config.entries().forEach(entry -> {
                    if (!entry.isDefault()) {
                        configMap.put(entry.name(), entry.value());
                    }
                });
                topicInfo.setConfig(configMap);
                
                // Parse and format retention time
                String retentionMs = configMap.getOrDefault("retention.ms", 
                    config.entries().stream()
                        .filter(e -> "retention.ms".equals(e.name()))
                        .map(e -> e.value())
                        .findFirst()
                        .orElse("-1"));
                topicInfo.setRetentionTime(formatRetentionTime(retentionMs));
            }

            return topicInfo;
        } catch (Exception e) {
            logger.error("Failed to get topic info: " + topicName, e);
            return null;
        }
    }

    /**
     * Create topic
     */
    public boolean createTopic(String clusterId, String topicName, int partitions, short replicationFactor) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            return false;
        }

        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            CreateTopicsResult result = admin.createTopics(Collections.singleton(newTopic));
            result.all().get(10, TimeUnit.SECONDS);
            logger.info("Topic created successfully: {}", topicName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to create topic: " + topicName, e);
            return false;
        }
    }

    /**
     * Delete topic
     */
    public boolean deleteTopic(String clusterId, String topicName) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            return false;
        }

        try {
            DeleteTopicsResult result = admin.deleteTopics(Collections.singleton(topicName));
            result.all().get(10, TimeUnit.SECONDS);
            logger.info("Topic deleted successfully: {}", topicName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete topic: " + topicName, e);
            return false;
        }
    }

    /**
     * Convert partition information
     */
    private TopicInfo.PartitionInfo convertPartitionInfo(TopicPartitionInfo kafkaPartitionInfo) {
        TopicInfo.PartitionInfo partitionInfo = new TopicInfo.PartitionInfo();
        partitionInfo.setPartition(kafkaPartitionInfo.partition());

        if (kafkaPartitionInfo.leader() != null) {
            partitionInfo.setLeader(convertNode(kafkaPartitionInfo.leader()));
        }

        List<TopicInfo.Node> replicas = kafkaPartitionInfo.replicas().stream()
                .map(this::convertNode)
                .collect(Collectors.toList());
        partitionInfo.setReplicas(replicas);

        List<TopicInfo.Node> isr = kafkaPartitionInfo.isr().stream()
                .map(this::convertNode)
                .collect(Collectors.toList());
        partitionInfo.setIsr(isr);

        return partitionInfo;
    }

    /**
     * Convert node information
     */
    private TopicInfo.Node convertNode(org.apache.kafka.common.Node kafkaNode) {
        return new TopicInfo.Node(kafkaNode.id(), kafkaNode.host(), kafkaNode.port());
    }
    
    /**
     * Format retention time from milliseconds to human-readable format
     */
    private String formatRetentionTime(String retentionMs) {
        try {
            long ms = Long.parseLong(retentionMs);
            if (ms < 0) {
                return "Infinite";
            }
            
            long days = ms / (1000 * 60 * 60 * 24);
            if (days > 0) {
                return days + " days";
            }
            
            long hours = ms / (1000 * 60 * 60);
            if (hours > 0) {
                return hours + " hours";
            }
            
            long minutes = ms / (1000 * 60);
            if (minutes > 0) {
                return minutes + " minutes";
            }
            
            return ms + " ms";
        } catch (NumberFormatException e) {
            return "N/A";
        }
    }
}
