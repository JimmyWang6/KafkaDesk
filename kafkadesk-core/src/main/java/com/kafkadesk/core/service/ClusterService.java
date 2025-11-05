package com.kafkadesk.core.service;

import com.kafkadesk.model.ClusterConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 集群服务
 */
public class ClusterService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
    private static ClusterService instance;
    private final Map<String, Admin> adminClients = new ConcurrentHashMap<>();

    private ClusterService() {
    }

    public static synchronized ClusterService getInstance() {
        if (instance == null) {
            instance = new ClusterService();
        }
        return instance;
    }

    /**
     * 连接到集群
     */
    public boolean connect(ClusterConfig config) {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
            props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

            // 添加安全配置
            if (config.getSecurityProtocol() != null) {
                props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, config.getSecurityProtocol());
            }
            if (config.getSaslMechanism() != null) {
                props.put("sasl.mechanism", config.getSaslMechanism());
            }

            // 添加自定义属性
            if (config.getProperties() != null) {
                props.putAll(config.getProperties());
            }

            Admin admin = Admin.create(props);
            
            // 测试连接
            admin.describeCluster().clusterId().get(10, TimeUnit.SECONDS);
            
            // 关闭旧连接
            closeConnection(config.getId());
            
            adminClients.put(config.getId(), admin);
            logger.info("Successfully connected to cluster: {}", config.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to cluster: " + config.getName(), e);
            return false;
        }
    }

    /**
     * 测试连接
     */
    public boolean testConnection(ClusterConfig config) {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");

            try (Admin admin = Admin.create(props)) {
                admin.describeCluster().clusterId().get(5, TimeUnit.SECONDS);
                return true;
            }
        } catch (Exception e) {
            logger.error("Connection test failed for cluster: " + config.getName(), e);
            return false;
        }
    }

    /**
     * 关闭连接
     */
    public void closeConnection(String clusterId) {
        Admin admin = adminClients.remove(clusterId);
        if (admin != null) {
            try {
                admin.close();
                logger.info("Closed connection to cluster: {}", clusterId);
            } catch (Exception e) {
                logger.error("Error closing admin client", e);
            }
        }
    }

    /**
     * 关闭所有连接
     */
    public void closeAllConnections() {
        adminClients.forEach((id, admin) -> {
            try {
                admin.close();
            } catch (Exception e) {
                logger.error("Error closing admin client for cluster: " + id, e);
            }
        });
        adminClients.clear();
    }

    /**
     * 获取 Admin 客户端
     */
    public Admin getAdminClient(String clusterId) {
        return adminClients.get(clusterId);
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected(String clusterId) {
        return adminClients.containsKey(clusterId);
    }

    /**
     * 获取集群信息
     */
    public Map<String, Object> getClusterInfo(String clusterId) {
        Admin admin = adminClients.get(clusterId);
        if (admin == null) {
            return null;
        }

        try {
            DescribeClusterResult result = admin.describeCluster();
            Map<String, Object> info = new HashMap<>();
            
            String clusterId1 = result.clusterId().get(5, TimeUnit.SECONDS);
            Node controller = result.controller().get(5, TimeUnit.SECONDS);
            Collection<Node> nodes = result.nodes().get(5, TimeUnit.SECONDS);
            
            info.put("clusterId", clusterId1);
            info.put("controller", controller);
            info.put("nodes", nodes);
            info.put("nodeCount", nodes.size());
            
            return info;
        } catch (Exception e) {
            logger.error("Failed to get cluster info", e);
            return null;
        }
    }
}
