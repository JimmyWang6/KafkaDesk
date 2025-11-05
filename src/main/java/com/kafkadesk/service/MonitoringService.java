package com.kafkadesk.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for monitoring Kafka cluster performance
 */
public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final KafkaConnectionService connectionService;

    public MonitoringService(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Get partition metrics for a topic
     */
    public Map<Integer, PartitionMetrics> getTopicPartitionMetrics(String connectionId, String topicName) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        // Get topic description
        DescribeTopicsResult describeResult = adminClient.describeTopics(Collections.singleton(topicName));
        TopicDescription topicDesc = describeResult.all().get().get(topicName);

        Map<Integer, PartitionMetrics> metricsMap = new HashMap<>();

        for (TopicPartitionInfo partitionInfo : topicDesc.partitions()) {
            int partition = partitionInfo.partition();
            TopicPartition tp = new TopicPartition(topicName, partition);

            PartitionMetrics metrics = new PartitionMetrics();
            metrics.setPartition(partition);
            metrics.setLeader(partitionInfo.leader() != null ? 
                            partitionInfo.leader().id() : -1);
            metrics.setReplicaCount(partitionInfo.replicas().size());
            metrics.setInSyncReplicaCount(partitionInfo.isr().size());

            // Get offsets
            Map<TopicPartition, OffsetSpec> earliestSpec = Collections.singletonMap(tp, OffsetSpec.earliest());
            Map<TopicPartition, OffsetSpec> latestSpec = Collections.singletonMap(tp, OffsetSpec.latest());

            ListOffsetsResult earliestResult = adminClient.listOffsets(earliestSpec);
            ListOffsetsResult latestResult = adminClient.listOffsets(latestSpec);

            long earliestOffset = earliestResult.all().get().get(tp).offset();
            long latestOffset = latestResult.all().get().get(tp).offset();

            metrics.setStartOffset(earliestOffset);
            metrics.setEndOffset(latestOffset);
            metrics.setMessageCount(latestOffset - earliestOffset);

            metricsMap.put(partition, metrics);
        }

        logger.info("Retrieved metrics for {} partitions of topic: {}", metricsMap.size(), topicName);
        return metricsMap;
    }

    /**
     * Get cluster node information
     */
    public List<NodeInfo> getClusterNodes(String connectionId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeClusterResult clusterResult = adminClient.describeCluster();
        Collection<org.apache.kafka.common.Node> nodes = clusterResult.nodes().get();

        return nodes.stream()
                .map(node -> {
                    NodeInfo info = new NodeInfo();
                    info.setId(node.id());
                    info.setHost(node.host());
                    info.setPort(node.port());
                    info.setRack(node.rack());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get cluster ID
     */
    public String getClusterId(String connectionId) throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeClusterResult clusterResult = adminClient.describeCluster();
        return clusterResult.clusterId().get();
    }

    /**
     * Get controller node
     */
    public NodeInfo getController(String connectionId) throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeClusterResult clusterResult = adminClient.describeCluster();
        org.apache.kafka.common.Node controller = clusterResult.controller().get();

        NodeInfo info = new NodeInfo();
        info.setId(controller.id());
        info.setHost(controller.host());
        info.setPort(controller.port());
        info.setRack(controller.rack());
        return info;
    }

    /**
     * Inner class for partition metrics
     */
    public static class PartitionMetrics {
        private int partition;
        private int leader;
        private int replicaCount;
        private int inSyncReplicaCount;
        private long startOffset;
        private long endOffset;
        private long messageCount;

        public int getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        public int getLeader() {
            return leader;
        }

        public void setLeader(int leader) {
            this.leader = leader;
        }

        public int getReplicaCount() {
            return replicaCount;
        }

        public void setReplicaCount(int replicaCount) {
            this.replicaCount = replicaCount;
        }

        public int getInSyncReplicaCount() {
            return inSyncReplicaCount;
        }

        public void setInSyncReplicaCount(int inSyncReplicaCount) {
            this.inSyncReplicaCount = inSyncReplicaCount;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(long endOffset) {
            this.endOffset = endOffset;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }
    }

    /**
     * Inner class for node information
     */
    public static class NodeInfo {
        private int id;
        private String host;
        private int port;
        private String rack;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getRack() {
            return rack;
        }

        public void setRack(String rack) {
            this.rack = rack;
        }

        @Override
        public String toString() {
            return "Node " + id + " (" + host + ":" + port + ")";
        }
    }
}
