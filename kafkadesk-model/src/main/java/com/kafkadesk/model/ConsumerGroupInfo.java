package com.kafkadesk.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者组信息模型
 */
public class ConsumerGroupInfo {
    private String groupId;
    private String state;
    private String protocolType;
    private int coordinatorId;
    private String coordinatorHost;
    private int coordinatorPort;
    private List<MemberInfo> members;
    private Map<TopicPartition, OffsetAndMetadata> offsets;
    private Map<TopicPartition, Long> lag;

    public ConsumerGroupInfo() {
        this.members = new ArrayList<>();
        this.offsets = new HashMap<>();
        this.lag = new HashMap<>();
    }

    public ConsumerGroupInfo(String groupId) {
        this();
        this.groupId = groupId;
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public List<MemberInfo> getMembers() {
        return members;
    }

    public void setMembers(List<MemberInfo> members) {
        this.members = members;
    }

    public Map<TopicPartition, OffsetAndMetadata> getOffsets() {
        return offsets;
    }

    public void setOffsets(Map<TopicPartition, OffsetAndMetadata> offsets) {
        this.offsets = offsets;
    }

    public Map<TopicPartition, Long> getLag() {
        return lag;
    }

    public void setLag(Map<TopicPartition, Long> lag) {
        this.lag = lag;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public void setCoordinatorId(int coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public String getCoordinatorHost() {
        return coordinatorHost;
    }

    public void setCoordinatorHost(String coordinatorHost) {
        this.coordinatorHost = coordinatorHost;
    }

    public int getCoordinatorPort() {
        return coordinatorPort;
    }

    public void setCoordinatorPort(int coordinatorPort) {
        this.coordinatorPort = coordinatorPort;
    }

    /**
     * 消费者组成员信息
     */
    public static class MemberInfo {
        private String memberId;
        private String clientId;
        private String host;
        private List<TopicPartition> assignments;

        public MemberInfo() {
            this.assignments = new ArrayList<>();
        }

        public String getMemberId() {
            return memberId;
        }

        public void setMemberId(String memberId) {
            this.memberId = memberId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public List<TopicPartition> getAssignments() {
            return assignments;
        }

        public void setAssignments(List<TopicPartition> assignments) {
            this.assignments = assignments;
        }
    }

    /**
     * 主题分区
     */
    public static class TopicPartition {
        private String topic;
        private int partition;

        public TopicPartition() {
        }

        public TopicPartition(String topic, int partition) {
            this.topic = topic;
            this.partition = partition;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public int getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        @Override
        public String toString() {
            return topic + "-" + partition;
        }
    }

    /**
     * Offset 和元数据
     */
    public static class OffsetAndMetadata {
        private long offset;
        private String metadata;

        public OffsetAndMetadata() {
        }

        public OffsetAndMetadata(long offset) {
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }
}
