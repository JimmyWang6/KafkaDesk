package com.kafkadesk.model;

/**
 * Represents a consumer group with its metadata
 */
public class ConsumerGroupInfo {
    private String groupId;
    private String state;
    private int memberCount;
    private String coordinator;
    private long lag;

    public ConsumerGroupInfo() {
    }

    public ConsumerGroupInfo(String groupId, String state) {
        this.groupId = groupId;
        this.state = state;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    public long getLag() {
        return lag;
    }

    public void setLag(long lag) {
        this.lag = lag;
    }

    @Override
    public String toString() {
        return groupId + " (" + state + ")";
    }
}
