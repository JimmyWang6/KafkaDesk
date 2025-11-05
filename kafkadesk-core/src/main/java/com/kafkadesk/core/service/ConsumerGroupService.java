package com.kafkadesk.core.service;

import com.kafkadesk.model.ConsumerGroupInfo;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Consumer group service
 */
public class ConsumerGroupService {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerGroupService.class);
    private static ConsumerGroupService instance;

    private ConsumerGroupService() {
    }

    public static synchronized ConsumerGroupService getInstance() {
        if (instance == null) {
            instance = new ConsumerGroupService();
        }
        return instance;
    }

    /**
     * List all consumer groups
     */
    public List<String> listConsumerGroups(String clusterId) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            logger.error("Admin client not found for cluster: {}", clusterId);
            return Collections.emptyList();
        }

        try {
            ListConsumerGroupsResult result = admin.listConsumerGroups();
            Collection<ConsumerGroupListing> listings = result.all().get(10, TimeUnit.SECONDS);
            return listings.stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list consumer groups", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get consumer group details
     */
    public ConsumerGroupInfo getConsumerGroupInfo(String clusterId, String groupId) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            return null;
        }

        try {
            DescribeConsumerGroupsResult describeResult = admin.describeConsumerGroups(
                    Collections.singleton(groupId)
            );
            ConsumerGroupDescription description = describeResult.all()
                    .get(10, TimeUnit.SECONDS)
                    .get(groupId);

            if (description == null) {
                return null;
            }

            ConsumerGroupInfo info = new ConsumerGroupInfo(groupId);
            info.setState(description.state().toString());
            info.setProtocolType(description.partitionAssignor());

            // 获取协调器信息
            if (description.coordinator() != null) {
                info.setCoordinatorId(description.coordinator().id());
                info.setCoordinatorHost(description.coordinator().host());
                info.setCoordinatorPort(description.coordinator().port());
            }

            // 转换成员信息
            List<ConsumerGroupInfo.MemberInfo> members = description.members().stream()
                    .map(this::convertMemberDescription)
                    .collect(Collectors.toList());
            info.setMembers(members);

            // 获取 offset 信息
            ListConsumerGroupOffsetsResult offsetsResult = admin.listConsumerGroupOffsets(groupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata()
                    .get(10, TimeUnit.SECONDS);

            Map<ConsumerGroupInfo.TopicPartition, ConsumerGroupInfo.OffsetAndMetadata> offsetMap = new HashMap<>();
            offsets.forEach((tp, om) -> {
                ConsumerGroupInfo.TopicPartition topicPartition = 
                        new ConsumerGroupInfo.TopicPartition(tp.topic(), tp.partition());
                ConsumerGroupInfo.OffsetAndMetadata offsetAndMetadata = 
                        new ConsumerGroupInfo.OffsetAndMetadata(om.offset());
                offsetAndMetadata.setMetadata(om.metadata());
                offsetMap.put(topicPartition, offsetAndMetadata);
            });
            info.setOffsets(offsetMap);

            // Calculate lag
            Map<ConsumerGroupInfo.TopicPartition, Long> lagMap = calculateLag(admin, offsets);
            info.setLag(lagMap);

            return info;
        } catch (Exception e) {
            logger.error("Failed to get consumer group info: " + groupId, e);
            return null;
        }
    }

    /**
     * Convert member description
     */
    private ConsumerGroupInfo.MemberInfo convertMemberDescription(MemberDescription member) {
        ConsumerGroupInfo.MemberInfo info = new ConsumerGroupInfo.MemberInfo();
        info.setMemberId(member.consumerId());
        info.setClientId(member.clientId());
        info.setHost(member.host());

        List<ConsumerGroupInfo.TopicPartition> assignments = member.assignment()
                .topicPartitions()
                .stream()
                .map(tp -> new ConsumerGroupInfo.TopicPartition(tp.topic(), tp.partition()))
                .collect(Collectors.toList());
        info.setAssignments(assignments);

        return info;
    }

    /**
     * Calculate lag
     */
    private Map<ConsumerGroupInfo.TopicPartition, Long> calculateLag(
            Admin admin, 
            Map<TopicPartition, OffsetAndMetadata> offsets) {
        
        Map<ConsumerGroupInfo.TopicPartition, Long> lagMap = new HashMap<>();
        
        if (offsets.isEmpty()) {
            return lagMap;
        }

        try {
            // 获取每个分区的最新 offset
            Map<TopicPartition, OffsetSpec> offsetSpecs = new HashMap<>();
            offsets.keySet().forEach(tp -> offsetSpecs.put(tp, OffsetSpec.latest()));

            ListOffsetsResult latestOffsetsResult = admin.listOffsets(offsetSpecs);
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = 
                    latestOffsetsResult.all().get(10, TimeUnit.SECONDS);

            // 计算每个分区的 lag
            offsets.forEach((tp, om) -> {
                ListOffsetsResult.ListOffsetsResultInfo latestOffset = latestOffsets.get(tp);
                if (latestOffset != null) {
                    long lag = latestOffset.offset() - om.offset();
                    ConsumerGroupInfo.TopicPartition topicPartition = 
                            new ConsumerGroupInfo.TopicPartition(tp.topic(), tp.partition());
                    lagMap.put(topicPartition, Math.max(0, lag));
                }
            });
        } catch (Exception e) {
            logger.error("Failed to calculate lag", e);
        }

        return lagMap;
    }

    /**
     * Delete consumer group
     */
    public boolean deleteConsumerGroup(String clusterId, String groupId) {
        Admin admin = ClusterService.getInstance().getAdminClient(clusterId);
        if (admin == null) {
            return false;
        }

        try {
            DeleteConsumerGroupsResult result = admin.deleteConsumerGroups(
                    Collections.singleton(groupId)
            );
            result.all().get(10, TimeUnit.SECONDS);
            logger.info("Consumer group deleted: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete consumer group: " + groupId, e);
            return false;
        }
    }
}
