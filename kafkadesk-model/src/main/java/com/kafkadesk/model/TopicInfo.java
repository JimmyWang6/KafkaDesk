package com.kafkadesk.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 主题信息模型
 */
public class TopicInfo {
    private String name;
    private int partitions;
    private int replicationFactor;
    private Map<String, String> config;
    private List<PartitionInfo> partitionDetails;

    public TopicInfo() {
        this.config = new HashMap<>();
        this.partitionDetails = new ArrayList<>();
    }

    public TopicInfo(String name, int partitions, int replicationFactor) {
        this();
        this.name = name;
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public List<PartitionInfo> getPartitionDetails() {
        return partitionDetails;
    }

    public void setPartitionDetails(List<PartitionInfo> partitionDetails) {
        this.partitionDetails = partitionDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicInfo topicInfo = (TopicInfo) o;
        return Objects.equals(name, topicInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TopicInfo{" +
                "name='" + name + '\'' +
                ", partitions=" + partitions +
                ", replicationFactor=" + replicationFactor +
                '}';
    }

    /**
     * 分区信息
     */
    public static class PartitionInfo {
        private int partition;
        private Node leader;
        private List<Node> replicas;
        private List<Node> isr;

        public PartitionInfo() {
            this.replicas = new ArrayList<>();
            this.isr = new ArrayList<>();
        }

        public int getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        public Node getLeader() {
            return leader;
        }

        public void setLeader(Node leader) {
            this.leader = leader;
        }

        public List<Node> getReplicas() {
            return replicas;
        }

        public void setReplicas(List<Node> replicas) {
            this.replicas = replicas;
        }

        public List<Node> getIsr() {
            return isr;
        }

        public void setIsr(List<Node> isr) {
            this.isr = isr;
        }
    }

    /**
     * 节点信息
     */
    public static class Node {
        private int id;
        private String host;
        private int port;

        public Node() {
        }

        public Node(int id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
        }

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

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
