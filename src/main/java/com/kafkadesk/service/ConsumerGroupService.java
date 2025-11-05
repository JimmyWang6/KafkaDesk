package com.kafkadesk.service;

import com.kafkadesk.model.ConsumerGroupInfo;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for monitoring consumer groups
 */
public class ConsumerGroupService {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerGroupService.class);

    private final KafkaConnectionService connectionService;

    public ConsumerGroupService(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * List all consumer groups
     */
    public List<ConsumerGroupInfo> listConsumerGroups(String connectionId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ListConsumerGroupsResult listResult = adminClient.listConsumerGroups();
        Collection<ConsumerGroupListing> listings = listResult.all().get();

        Set<String> groupIds = listings.stream()
                .map(ConsumerGroupListing::groupId)
                .collect(Collectors.toSet());

        DescribeConsumerGroupsResult describeResult = adminClient.describeConsumerGroups(groupIds);
        Map<String, ConsumerGroupDescription> descriptions = describeResult.all().get();

        List<ConsumerGroupInfo> groups = new ArrayList<>();
        for (Map.Entry<String, ConsumerGroupDescription> entry : descriptions.entrySet()) {
            ConsumerGroupDescription desc = entry.getValue();
            ConsumerGroupInfo info = new ConsumerGroupInfo();
            info.setGroupId(desc.groupId());
            info.setState(desc.state().toString());
            info.setMemberCount(desc.members().size());
            
            if (desc.coordinator() != null) {
                info.setCoordinator(desc.coordinator().host() + ":" + desc.coordinator().port());
            }
            
            groups.add(info);
        }

        logger.info("Listed {} consumer groups", groups.size());
        return groups;
    }

    /**
     * Get consumer group details
     */
    public ConsumerGroupInfo getConsumerGroupDetails(String connectionId, String groupId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DescribeConsumerGroupsResult describeResult = adminClient.describeConsumerGroups(
                Collections.singleton(groupId));
        ConsumerGroupDescription desc = describeResult.all().get().get(groupId);

        ConsumerGroupInfo info = new ConsumerGroupInfo();
        info.setGroupId(desc.groupId());
        info.setState(desc.state().toString());
        info.setMemberCount(desc.members().size());
        
        if (desc.coordinator() != null) {
            info.setCoordinator(desc.coordinator().host() + ":" + desc.coordinator().port());
        }

        // Calculate lag
        long totalLag = calculateLag(adminClient, groupId);
        info.setLag(totalLag);

        return info;
    }

    /**
     * Get consumer group offsets
     */
    public Map<TopicPartition, OffsetAndMetadata> getConsumerGroupOffsets(String connectionId, String groupId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
        return offsetsResult.partitionsToOffsetAndMetadata().get();
    }

    /**
     * Delete consumer group
     */
    public void deleteConsumerGroup(String connectionId, String groupId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        DeleteConsumerGroupsResult result = adminClient.deleteConsumerGroups(
                Collections.singleton(groupId));
        result.all().get();
        
        logger.info("Deleted consumer group: {}", groupId);
    }

    /**
     * Reset consumer group offsets
     */
    public void resetConsumerGroupOffsets(String connectionId, String groupId, 
                                         Map<TopicPartition, Long> offsets) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        Map<TopicPartition, OffsetAndMetadata> offsetsToReset = offsets.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new OffsetAndMetadata(e.getValue())
                ));

        AlterConsumerGroupOffsetsResult result = adminClient.alterConsumerGroupOffsets(
                groupId, offsetsToReset);
        result.all().get();
        
        logger.info("Reset offsets for consumer group: {}", groupId);
    }

    /**
     * Calculate total lag for a consumer group
     */
    private long calculateLag(AdminClient adminClient, String groupId) {
        try {
            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get();

            if (offsets.isEmpty()) {
                return 0;
            }

            Map<TopicPartition, OffsetSpec> offsetSpecs = offsets.keySet().stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            ListOffsetsResult latestOffsetsResult = adminClient.listOffsets(offsetSpecs);
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = 
                    latestOffsetsResult.all().get();

            long totalLag = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long consumerOffset = entry.getValue().offset();
                
                ListOffsetsResult.ListOffsetsResultInfo latestInfo = latestOffsets.get(tp);
                if (latestInfo != null) {
                    long latestOffset = latestInfo.offset();
                    totalLag += Math.max(0, latestOffset - consumerOffset);
                }
            }

            return totalLag;
        } catch (Exception e) {
            logger.warn("Failed to calculate lag for group: {}", groupId, e);
            return 0;
        }
    }

    /**
     * Get lag for each partition of a consumer group
     */
    public Map<TopicPartition, Long> getConsumerGroupLag(String connectionId, String groupId) 
            throws ExecutionException, InterruptedException {
        AdminClient adminClient = connectionService.getAdminClient(connectionId);
        if (adminClient == null) {
            throw new IllegalStateException("Not connected to cluster");
        }

        ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
        Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get();

        if (offsets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<TopicPartition, OffsetSpec> offsetSpecs = offsets.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

        ListOffsetsResult latestOffsetsResult = adminClient.listOffsets(offsetSpecs);
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = 
                latestOffsetsResult.all().get();

        Map<TopicPartition, Long> lagMap = new HashMap<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
            TopicPartition tp = entry.getKey();
            long consumerOffset = entry.getValue().offset();
            
            ListOffsetsResult.ListOffsetsResultInfo latestInfo = latestOffsets.get(tp);
            if (latestInfo != null) {
                long latestOffset = latestInfo.offset();
                lagMap.put(tp, Math.max(0, latestOffset - consumerOffset));
            }
        }

        return lagMap;
    }
}
