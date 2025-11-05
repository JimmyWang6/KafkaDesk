package com.kafkadesk.ui.controller;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.core.service.ClusterService;
import com.kafkadesk.core.service.TopicService;
import com.kafkadesk.model.ClusterConfig;
import com.kafkadesk.model.TopicInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 主窗口控制器
 */
public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TreeView<String> clusterTreeView;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab topicTab;

    @FXML
    private Tab producerTab;

    @FXML
    private Tab consumerTab;

    @FXML
    private TableView<TopicInfo> topicTableView;

    @FXML
    private TableColumn<TopicInfo, String> topicNameColumn;

    @FXML
    private TableColumn<TopicInfo, Integer> topicPartitionsColumn;

    @FXML
    private TableColumn<TopicInfo, Integer> topicReplicationColumn;

    @FXML
    private TextArea topicDetailsTextArea;

    @FXML
    private TextField producerTopicField;

    @FXML
    private TextArea producerMessageTextArea;

    @FXML
    private TextField producerKeyField;

    @FXML
    private Button sendMessageButton;

    @FXML
    private ComboBox<String> consumerTopicComboBox;

    @FXML
    private TableView<MessageRow> consumerTableView;

    @FXML
    private TableColumn<MessageRow, Long> offsetColumn;

    @FXML
    private TableColumn<MessageRow, String> keyColumn;

    @FXML
    private TableColumn<MessageRow, String> valueColumn;

    @FXML
    private TableColumn<MessageRow, String> timestampColumn;

    @FXML
    private TextArea consumerDetailsTextArea;

    @FXML
    private Label statusLabel;

    private Stage stage;
    private ClusterConfig currentCluster;
    private final ObservableList<TopicInfo> topicList = FXCollections.observableArrayList();
    private final ObservableList<MessageRow> messageList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");

        // 初始化集群树
        initializeClusterTree();

        // 初始化主题表格
        initializeTopicTable();

        // 初始化生产者界面
        initializeProducerView();

        // 初始化消费者界面
        initializeConsumerView();

        // 设置状态栏
        updateStatus("就绪");
    }

    /**
     * 初始化集群树
     */
    private void initializeClusterTree() {
        TreeItem<String> rootItem = new TreeItem<>("集群列表");
        rootItem.setExpanded(true);

        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        for (ClusterConfig cluster : clusters) {
            TreeItem<String> clusterItem = new TreeItem<>(cluster.getName());
            rootItem.getChildren().add(clusterItem);
        }

        clusterTreeView.setRoot(rootItem);
        clusterTreeView.setShowRoot(true);

        // 添加选择监听器
        clusterTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getParent() != null && newVal.getParent() == rootItem) {
                onClusterSelected(newVal.getValue());
            }
        });
    }

    /**
     * 集群被选中
     */
    private void onClusterSelected(String clusterName) {
        logger.info("Cluster selected: {}", clusterName);
        
        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        Optional<ClusterConfig> cluster = clusters.stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst();

        if (cluster.isPresent()) {
            currentCluster = cluster.get();
            connectToCluster(currentCluster);
        }
    }

    /**
     * 连接到集群
     */
    private void connectToCluster(ClusterConfig config) {
        updateStatus("正在连接到集群: " + config.getName());

        new Thread(() -> {
            boolean connected = ClusterService.getInstance().connect(config);
            
            Platform.runLater(() -> {
                if (connected) {
                    updateStatus("已连接到集群: " + config.getName());
                    loadTopics();
                } else {
                    updateStatus("连接失败: " + config.getName());
                    showError("连接失败", "无法连接到集群: " + config.getName());
                }
            });
        }).start();
    }

    /**
     * 初始化主题表格
     */
    private void initializeTopicTable() {
        topicNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        topicPartitionsColumn.setCellValueFactory(new PropertyValueFactory<>("partitions"));
        topicReplicationColumn.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));

        topicTableView.setItems(topicList);

        // 添加选择监听器
        topicTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showTopicDetails(newVal);
            }
        });
    }

    /**
     * 加载主题列表
     */
    private void loadTopics() {
        if (currentCluster == null) {
            return;
        }

        updateStatus("正在加载主题列表...");

        new Thread(() -> {
            List<String> topicNames = TopicService.getInstance().listTopics(currentCluster.getId());
            
            Platform.runLater(() -> {
                topicList.clear();
                for (String topicName : topicNames) {
                    TopicInfo info = new TopicInfo();
                    info.setName(topicName);
                    info.setPartitions(0);  // 将在详情中加载
                    info.setReplicationFactor(0);
                    topicList.add(info);
                }
                updateStatus("已加载 " + topicNames.size() + " 个主题");
            });
        }).start();
    }

    /**
     * 显示主题详情
     */
    private void showTopicDetails(TopicInfo topic) {
        if (currentCluster == null) {
            return;
        }

        new Thread(() -> {
            TopicInfo fullInfo = TopicService.getInstance().getTopicInfo(
                    currentCluster.getId(), 
                    topic.getName()
            );

            Platform.runLater(() -> {
                if (fullInfo != null) {
                    // 更新表格中的信息
                    topic.setPartitions(fullInfo.getPartitions());
                    topic.setReplicationFactor(fullInfo.getReplicationFactor());
                    topicTableView.refresh();

                    // 显示详细信息
                    StringBuilder details = new StringBuilder();
                    details.append("主题名称: ").append(fullInfo.getName()).append("\n");
                    details.append("分区数: ").append(fullInfo.getPartitions()).append("\n");
                    details.append("副本因子: ").append(fullInfo.getReplicationFactor()).append("\n\n");
                    details.append("配置:\n");
                    fullInfo.getConfig().forEach((key, value) -> 
                        details.append("  ").append(key).append(": ").append(value).append("\n")
                    );

                    topicDetailsTextArea.setText(details.toString());
                }
            });
        }).start();
    }

    /**
     * 初始化生产者界面
     */
    private void initializeProducerView() {
        sendMessageButton.setOnAction(event -> sendMessage());
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String topic = producerTopicField.getText();
        String message = producerMessageTextArea.getText();
        String key = producerKeyField.getText();

        if (topic.isEmpty() || message.isEmpty()) {
            showError("输入错误", "请填写主题和消息内容");
            return;
        }

        if (currentCluster == null) {
            showError("未连接", "请先连接到集群");
            return;
        }

        updateStatus("正在发送消息...");

        new Thread(() -> {
            try {
                com.kafkadesk.model.Message msg = new com.kafkadesk.model.Message();
                msg.setTopic(topic);
                msg.setValue(message);
                if (!key.isEmpty()) {
                    msg.setKey(key);
                }

                com.kafkadesk.core.service.ProducerService.getInstance()
                        .sendMessage(currentCluster.getBootstrapServers(), msg);

                Platform.runLater(() -> {
                    updateStatus("消息发送成功");
                    showInfo("成功", "消息已发送到主题: " + topic);
                    producerMessageTextArea.clear();
                });
            } catch (Exception e) {
                logger.error("Failed to send message", e);
                Platform.runLater(() -> {
                    updateStatus("消息发送失败");
                    showError("发送失败", "无法发送消息: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 初始化消费者界面
     */
    private void initializeConsumerView() {
        offsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        consumerTableView.setItems(messageList);
    }

    /**
     * 添加集群
     */
    @FXML
    private void handleAddCluster() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加集群");
        dialog.setHeaderText("添加新的 Kafka 集群");
        dialog.setContentText("集群名称:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            TextInputDialog serversDialog = new TextInputDialog("localhost:9092");
            serversDialog.setTitle("添加集群");
            serversDialog.setHeaderText("设置 Bootstrap Servers");
            serversDialog.setContentText("Bootstrap Servers:");

            Optional<String> serversResult = serversDialog.showAndWait();
            serversResult.ifPresent(servers -> {
                ClusterConfig config = new ClusterConfig(name, servers);
                ConfigManager.getInstance().addCluster(config);
                initializeClusterTree();
                showInfo("成功", "集群已添加: " + name);
            });
        });
    }

    /**
     * 刷新主题列表
     */
    @FXML
    private void handleRefreshTopics() {
        loadTopics();
    }

    /**
     * 关于对话框
     */
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 KafkaDesk");
        alert.setHeaderText("KafkaDesk v1.0.0");
        alert.setContentText("基于 Java 和 JavaFX 的 Kafka 桌面客户端工具\n\n" +
                "提供可视化的 Kafka 集群管理和操作功能");
        alert.showAndWait();
    }

    /**
     * 退出应用
     */
    @FXML
    private void handleExit() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 更新状态栏
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * 显示错误对话框
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息对话框
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 消息行数据类
     */
    public static class MessageRow {
        private Long offset;
        private String key;
        private String value;
        private String timestamp;

        public MessageRow(Long offset, String key, String value, String timestamp) {
            this.offset = offset;
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }

        public Long getOffset() {
            return offset;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}
