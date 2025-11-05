package com.kafkadesk.ui.controller;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.core.service.*;
import com.kafkadesk.model.ClusterConfig;
import com.kafkadesk.model.ConsumerGroupInfo;
import com.kafkadesk.model.Message;
import com.kafkadesk.model.TopicInfo;
import com.kafkadesk.ui.util.I18nUtil;
import com.kafkadesk.utils.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.*;

/**
 * Main Window Controller with i18n support
 */
public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Menu components
    @FXML private MenuBar menuBar;
    @FXML private Menu menuFile, menuView, menuTools, menuHelp;
    @FXML private MenuItem menuItemAddCluster, menuItemExit, menuItemRefresh, menuItemSettings, menuItemAbout;
    
    // Toolbar components
    @FXML private Button btnAddCluster, btnRefresh, btnSettings;
    
    // Cluster tree
    @FXML private Label lblClusterList;
    @FXML private TreeView<String> clusterTreeView;

    // Tabs
    @FXML private TabPane mainTabPane;
    @FXML private Tab topicTab, producerTab, queryTab, consumerGroupsTab;

    // Topic Management
    @FXML private Label lblTopicList, lblTopicDetails;
    @FXML private TableView<TopicInfo> topicTableView;
    @FXML private TableColumn<TopicInfo, String> topicNameColumn;
    @FXML private TableColumn<TopicInfo, Integer> topicPartitionsColumn, topicReplicationColumn;
    @FXML private TextArea topicDetailsTextArea;

    // Producer
    @FXML private Label lblProducerTitle, lblProducerTopic, lblProducerKey, lblProducerValue;
    @FXML private TextField producerTopicField, producerKeyField;
    @FXML private TextArea producerMessageTextArea;
    @FXML private Button btnSendMessage;

    // Query
    @FXML private Label lblQueryTopic, lblQueryPartition, lblQueryOffsetFrom, lblQueryOffsetTo, lblQueryMaxRecords;
    @FXML private Label lblQueryResults, lblQueryDetails;
    @FXML private ComboBox<String> queryTopicComboBox, queryPartitionComboBox;
    @FXML private TextField queryOffsetFromField, queryOffsetToField, queryMaxRecordsField;
    @FXML private Button btnQuerySearch, btnQueryClear, btnQueryExport;
    @FXML private TableView<MessageRow> queryTableView;
    @FXML private TableColumn<MessageRow, String> queryOffsetColumn, queryKeyColumn, queryValueColumn, queryTimestampColumn;
    @FXML private TextArea queryDetailsTextArea;

    // Consumer Groups
    @FXML private Label lblConsumerGroupList, lblConsumerGroupDetails;
    @FXML private TableView<ConsumerGroupRow> consumerGroupTableView;
    @FXML private TableColumn<ConsumerGroupRow, String> consumerGroupIdColumn, consumerGroupStateColumn;
    @FXML private TableColumn<ConsumerGroupRow, String> consumerGroupCoordinatorColumn;
    @FXML private TableColumn<ConsumerGroupRow, Integer> consumerGroupMembersColumn;
    
    @FXML private TableView<MemberRow> consumerGroupMembersTableView;
    @FXML private TableColumn<MemberRow, String> memberIdColumn, memberClientIdColumn, memberHostColumn, memberAssignmentsColumn;
    
    @FXML private TableView<LagRow> consumerGroupLagTableView;
    @FXML private TableColumn<LagRow, String> lagTopicColumn;
    @FXML private TableColumn<LagRow, Integer> lagPartitionColumn;
    @FXML private TableColumn<LagRow, Long> lagOffsetColumn, lagValueColumn;

    // Status bar
    @FXML private Label statusLabel;

    private Stage stage;
    private ClusterConfig currentCluster;
    private final ObservableList<TopicInfo> topicList = FXCollections.observableArrayList();
    private final ObservableList<MessageRow> messageList = FXCollections.observableArrayList();
    private final ObservableList<ConsumerGroupRow> consumerGroupList = FXCollections.observableArrayList();
    private final ObservableList<MemberRow> memberList = FXCollections.observableArrayList();
    private final ObservableList<LagRow> lagList = FXCollections.observableArrayList();
    
    private KafkaConsumer<String, String> queryConsumer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");
        
        // Initialize i18n from configuration
        String language = ConfigManager.getInstance().getConfig().getPreferences().getLanguage();
        I18nUtil.setLocale(language);

        initializeUI();
        initializeClusterTree();
        initializeTopicTable();
        initializeProducerView();
        initializeQueryView();
        initializeConsumerGroupView();
        
        updateStatus(I18nUtil.get("status.ready"));
    }

    /**
     * Initialize UI text with i18n
     */
    private void initializeUI() {
        // Menu
        menuFile.setText(I18nUtil.get("menu.file"));
        menuItemAddCluster.setText(I18nUtil.get("menu.file.addCluster"));
        menuItemExit.setText(I18nUtil.get("menu.file.exit"));
        
        menuView.setText(I18nUtil.get("menu.view"));
        menuItemRefresh.setText(I18nUtil.get("menu.view.refresh"));
        
        menuTools.setText(I18nUtil.get("menu.tools"));
        menuItemSettings.setText(I18nUtil.get("menu.tools.settings"));
        
        menuHelp.setText(I18nUtil.get("menu.help"));
        menuItemAbout.setText(I18nUtil.get("menu.help.about"));

        // Toolbar
        btnAddCluster.setText(I18nUtil.get("toolbar.addCluster"));
        btnRefresh.setText(I18nUtil.get("toolbar.refresh"));
        btnSettings.setText(I18nUtil.get("toolbar.settings"));

        // Cluster
        lblClusterList.setText(I18nUtil.get("cluster.list"));

        // Tabs
        topicTab.setText(I18nUtil.get("tab.topics"));
        producerTab.setText(I18nUtil.get("tab.producer"));
        queryTab.setText(I18nUtil.get("tab.query"));
        consumerGroupsTab.setText(I18nUtil.get("tab.consumerGroups"));

        // Topic
        lblTopicList.setText(I18nUtil.get("topic.list"));
        lblTopicDetails.setText(I18nUtil.get("topic.details"));
        topicNameColumn.setText(I18nUtil.get("topic.name"));
        topicPartitionsColumn.setText(I18nUtil.get("topic.partitions"));
        topicReplicationColumn.setText(I18nUtil.get("topic.replication"));

        // Producer
        lblProducerTitle.setText(I18nUtil.get("producer.title"));
        lblProducerTopic.setText(I18nUtil.get("producer.topic"));
        lblProducerKey.setText(I18nUtil.get("producer.key"));
        lblProducerValue.setText(I18nUtil.get("producer.value"));
        producerTopicField.setPromptText(I18nUtil.get("producer.topic.prompt"));
        producerKeyField.setPromptText(I18nUtil.get("producer.key.prompt"));
        producerMessageTextArea.setPromptText(I18nUtil.get("producer.value.prompt"));
        btnSendMessage.setText(I18nUtil.get("producer.send"));

        // Query
        lblQueryTopic.setText(I18nUtil.get("query.topic"));
        lblQueryPartition.setText(I18nUtil.get("query.partition"));
        lblQueryOffsetFrom.setText(I18nUtil.get("query.offset.from"));
        lblQueryOffsetTo.setText(I18nUtil.get("query.offset.to"));
        lblQueryMaxRecords.setText(I18nUtil.get("query.maxRecords"));
        lblQueryResults.setText(I18nUtil.get("query.results"));
        lblQueryDetails.setText(I18nUtil.get("query.details"));
        queryTopicComboBox.setPromptText(I18nUtil.get("query.topic.prompt"));
        btnQuerySearch.setText(I18nUtil.get("query.search"));
        btnQueryClear.setText(I18nUtil.get("query.clear"));
        btnQueryExport.setText(I18nUtil.get("query.export"));
        queryOffsetColumn.setText(I18nUtil.get("query.offset"));
        queryKeyColumn.setText(I18nUtil.get("query.key"));
        queryValueColumn.setText(I18nUtil.get("query.value"));
        queryTimestampColumn.setText(I18nUtil.get("query.timestamp"));

        // Consumer Groups
        lblConsumerGroupList.setText(I18nUtil.get("consumerGroup.list"));
        lblConsumerGroupDetails.setText(I18nUtil.get("consumerGroup.details"));
        consumerGroupIdColumn.setText(I18nUtil.get("consumerGroup.groupId"));
        consumerGroupStateColumn.setText(I18nUtil.get("consumerGroup.state"));
        consumerGroupCoordinatorColumn.setText(I18nUtil.get("consumerGroup.coordinator"));
        consumerGroupMembersColumn.setText(I18nUtil.get("consumerGroup.members"));
        
        memberIdColumn.setText(I18nUtil.get("consumerGroup.member.memberId"));
        memberClientIdColumn.setText(I18nUtil.get("consumerGroup.member.clientId"));
        memberHostColumn.setText(I18nUtil.get("consumerGroup.member.host"));
        memberAssignmentsColumn.setText(I18nUtil.get("consumerGroup.member.assignments"));
        
        lagTopicColumn.setText(I18nUtil.get("topic.name"));
        lagPartitionColumn.setText(I18nUtil.get("query.partition"));
        lagOffsetColumn.setText(I18nUtil.get("consumerGroup.offset"));
        lagValueColumn.setText(I18nUtil.get("consumerGroup.lag"));
    }

    private void initializeClusterTree() {
        TreeItem<String> rootItem = new TreeItem<>(I18nUtil.get("cluster.list"));
        rootItem.setExpanded(true);

        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        for (ClusterConfig cluster : clusters) {
            TreeItem<String> clusterItem = new TreeItem<>(cluster.getName());
            rootItem.getChildren().add(clusterItem);
        }

        clusterTreeView.setRoot(rootItem);
        clusterTreeView.setShowRoot(true);

        clusterTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getParent() != null && newVal.getParent() == rootItem) {
                onClusterSelected(newVal.getValue());
            }
        });
    }

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

    private void connectToCluster(ClusterConfig config) {
        updateStatus(I18nUtil.get("cluster.connecting", config.getName()));

        new Thread(() -> {
            boolean connected = ClusterService.getInstance().connect(config);
            
            Platform.runLater(() -> {
                if (connected) {
                    updateStatus(I18nUtil.get("cluster.connected", config.getName()));
                    loadTopics();
                    loadConsumerGroups();
                    updateQueryTopicList();
                } else {
                    updateStatus(I18nUtil.get("cluster.failed", config.getName()));
                    showError(I18nUtil.get("dialog.error.title"), 
                            I18nUtil.get("error.connectionFailed"));
                }
            });
        }).start();
    }

    private void initializeTopicTable() {
        topicNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        topicPartitionsColumn.setCellValueFactory(new PropertyValueFactory<>("partitions"));
        topicReplicationColumn.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));

        topicTableView.setItems(topicList);

        topicTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showTopicDetails(newVal);
            }
        });
    }

    private void loadTopics() {
        if (currentCluster == null) {
            return;
        }

        updateStatus(I18nUtil.get("topic.loading"));

        new Thread(() -> {
            List<String> topicNames = TopicService.getInstance().listTopics(currentCluster.getId());
            
            Platform.runLater(() -> {
                topicList.clear();
                for (String topicName : topicNames) {
                    TopicInfo info = new TopicInfo();
                    info.setName(topicName);
                    info.setPartitions(0);
                    info.setReplicationFactor(0);
                    topicList.add(info);
                }
                updateStatus(I18nUtil.get("topic.loaded", topicNames.size()));
            });
        }).start();
    }

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
                    topic.setPartitions(fullInfo.getPartitions());
                    topic.setReplicationFactor(fullInfo.getReplicationFactor());
                    topicTableView.refresh();

                    StringBuilder details = new StringBuilder();
                    details.append(I18nUtil.get("topic.name")).append(": ").append(fullInfo.getName()).append("\n");
                    details.append(I18nUtil.get("topic.partitions")).append(": ").append(fullInfo.getPartitions()).append("\n");
                    details.append(I18nUtil.get("topic.replication")).append(": ").append(fullInfo.getReplicationFactor()).append("\n\n");
                    details.append("Configuration:\n");
                    fullInfo.getConfig().forEach((key, value) -> 
                        details.append("  ").append(key).append(": ").append(value).append("\n")
                    );

                    topicDetailsTextArea.setText(details.toString());
                }
            });
        }).start();
    }

    private void initializeProducerView() {
        // Already set via FXML
    }

    @FXML
    private void handleSendMessage() {
        String topic = producerTopicField.getText();
        String message = producerMessageTextArea.getText();
        String key = producerKeyField.getText();

        if (topic.isEmpty() || message.isEmpty()) {
            showError(I18nUtil.get("producer.error.title"), I18nUtil.get("producer.error.required"));
            return;
        }

        if (currentCluster == null) {
            showError(I18nUtil.get("dialog.error.title"), I18nUtil.get("query.noConnection"));
            return;
        }

        updateStatus(I18nUtil.get("producer.sending"));

        new Thread(() -> {
            try {
                Message msg = new Message();
                msg.setTopic(topic);
                msg.setValue(message);
                if (!key.isEmpty()) {
                    msg.setKey(key);
                }

                ProducerService.getInstance().sendMessage(currentCluster.getBootstrapServers(), msg);

                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get("producer.success"));
                    showInfo(I18nUtil.get("common.success"), I18nUtil.get("producer.success"));
                    producerMessageTextArea.clear();
                });
            } catch (Exception e) {
                logger.error("Failed to send message", e);
                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get("producer.failed", e.getMessage()));
                    showError(I18nUtil.get("dialog.error.title"), 
                            I18nUtil.get("producer.failed", e.getMessage()));
                });
            }
        }).start();
    }

    private void initializeQueryView() {
        queryOffsetColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getOffset())));
        queryKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        queryValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        queryTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        queryTableView.setItems(messageList);
        
        queryTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showMessageDetails(newVal);
            }
        });
        
        queryPartitionComboBox.getItems().add(I18nUtil.get("query.partition.all"));
        queryPartitionComboBox.setValue(I18nUtil.get("query.partition.all"));
    }

    private void updateQueryTopicList() {
        if (currentCluster == null) {
            return;
        }

        queryTopicComboBox.getItems().clear();
        queryTopicComboBox.getItems().addAll(
            TopicService.getInstance().listTopics(currentCluster.getId())
        );
        
        queryTopicComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updatePartitionList(newVal);
            }
        });
    }

    private void updatePartitionList(String topic) {
        queryPartitionComboBox.getItems().clear();
        queryPartitionComboBox.getItems().add(I18nUtil.get("query.partition.all"));
        
        TopicInfo info = TopicService.getInstance().getTopicInfo(currentCluster.getId(), topic);
        if (info != null) {
            for (int i = 0; i < info.getPartitions(); i++) {
                queryPartitionComboBox.getItems().add(String.valueOf(i));
            }
        }
        queryPartitionComboBox.setValue(I18nUtil.get("query.partition.all"));
    }

    @FXML
    private void handleQueryMessages() {
        String topic = queryTopicComboBox.getValue();
        if (topic == null || topic.isEmpty()) {
            showError(I18nUtil.get("dialog.error.title"), I18nUtil.get("producer.error.required"));
            return;
        }

        if (currentCluster == null) {
            showError(I18nUtil.get("dialog.error.title"), I18nUtil.get("query.noConnection"));
            return;
        }

        messageList.clear();
        updateStatus(I18nUtil.get("query.searching"));

        new Thread(() -> {
            try {
                String partitionStr = queryPartitionComboBox.getValue();
                long fromOffset = Long.parseLong(queryOffsetFromField.getText().isEmpty() ? "0" : queryOffsetFromField.getText());
                long toOffset = Long.parseLong(queryOffsetToField.getText().isEmpty() ? "-1" : queryOffsetToField.getText());
                int maxRecords = Integer.parseInt(queryMaxRecordsField.getText().isEmpty() ? "100" : queryMaxRecordsField.getText());

                queryConsumer = ConsumerService.getInstance().createConsumer(
                    currentCluster.getBootstrapServers(), 
                    "kafkadesk-query-" + System.currentTimeMillis()
                );

                List<Integer> partitions = new ArrayList<>();
                if (partitionStr.equals(I18nUtil.get("query.partition.all"))) {
                    TopicInfo info = TopicService.getInstance().getTopicInfo(currentCluster.getId(), topic);
                    if (info != null) {
                        for (int i = 0; i < info.getPartitions(); i++) {
                            partitions.add(i);
                        }
                    }
                } else {
                    partitions.add(Integer.parseInt(partitionStr));
                }

                ConsumerService.getInstance().assignPartitions(queryConsumer, topic, partitions);

                for (int partition : partitions) {
                    queryConsumer.seek(new TopicPartition(topic, partition), fromOffset);
                }

                int recordCount = 0;
                boolean keepReading = true;

                while (keepReading && recordCount < maxRecords) {
                    ConsumerRecords<String, String> records = queryConsumer.poll(Duration.ofMillis(1000));
                    
                    for (ConsumerRecord<String, String> record : records) {
                        if (toOffset >= 0 && record.offset() > toOffset) {
                            keepReading = false;
                            break;
                        }
                        
                        MessageRow row = new MessageRow(
                            record.offset(),
                            record.key(),
                            record.value(),
                            DateTimeUtil.formatTimestamp(record.timestamp())
                        );
                        
                        Platform.runLater(() -> messageList.add(row));
                        recordCount++;
                        
                        if (recordCount >= maxRecords) {
                            keepReading = false;
                            break;
                        }
                    }
                    
                    if (records.isEmpty()) {
                        break;
                    }
                }

                int finalCount = recordCount;
                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get("query.found", finalCount));
                });

                ConsumerService.getInstance().closeConsumer(queryConsumer);
                queryConsumer = null;

            } catch (Exception e) {
                logger.error("Failed to query messages", e);
                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get("error.queryFailed"));
                    showError(I18nUtil.get("dialog.error.title"), 
                            I18nUtil.get("error.queryFailed") + ": " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleClearQuery() {
        messageList.clear();
        queryDetailsTextArea.clear();
    }

    @FXML
    private void handleExportQuery() {
        // TODO: Implement export functionality
        showInfo(I18nUtil.get("common.info"), "Export functionality coming soon");
    }

    private void showMessageDetails(MessageRow row) {
        StringBuilder details = new StringBuilder();
        details.append("Offset: ").append(row.getOffset()).append("\n");
        details.append("Key: ").append(row.getKey()).append("\n");
        details.append("Timestamp: ").append(row.getTimestamp()).append("\n\n");
        details.append("Value:\n").append(row.getValue());
        queryDetailsTextArea.setText(details.toString());
    }

    private void initializeConsumerGroupView() {
        consumerGroupIdColumn.setCellValueFactory(new PropertyValueFactory<>("groupId"));
        consumerGroupStateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        consumerGroupCoordinatorColumn.setCellValueFactory(new PropertyValueFactory<>("coordinator"));
        consumerGroupMembersColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));

        consumerGroupTableView.setItems(consumerGroupList);

        memberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberClientIdColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        memberHostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        memberAssignmentsColumn.setCellValueFactory(new PropertyValueFactory<>("assignments"));

        consumerGroupMembersTableView.setItems(memberList);

        lagTopicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        lagPartitionColumn.setCellValueFactory(new PropertyValueFactory<>("partition"));
        lagOffsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        lagValueColumn.setCellValueFactory(new PropertyValueFactory<>("lag"));

        consumerGroupLagTableView.setItems(lagList);

        consumerGroupTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showConsumerGroupDetails(newVal.getGroupId());
            }
        });
    }

    private void loadConsumerGroups() {
        if (currentCluster == null) {
            return;
        }

        updateStatus(I18nUtil.get("consumerGroup.loading"));

        new Thread(() -> {
            List<String> groupIds = ConsumerGroupService.getInstance().listConsumerGroups(currentCluster.getId());
            
            Platform.runLater(() -> {
                consumerGroupList.clear();
                for (String groupId : groupIds) {
                    ConsumerGroupRow row = new ConsumerGroupRow(groupId, "", "", 0);
                    consumerGroupList.add(row);
                }
                updateStatus(I18nUtil.get("consumerGroup.loaded", groupIds.size()));
                
                // Load details for each group in background
                loadConsumerGroupDetails();
            });
        }).start();
    }

    private void loadConsumerGroupDetails() {
        new Thread(() -> {
            for (ConsumerGroupRow row : consumerGroupList) {
                ConsumerGroupInfo info = ConsumerGroupService.getInstance()
                        .getConsumerGroupInfo(currentCluster.getId(), row.getGroupId());
                
                if (info != null) {
                    Platform.runLater(() -> {
                        row.setState(info.getState());
                        row.setCoordinator(info.getCoordinatorHost() + ":" + info.getCoordinatorPort());
                        row.setMemberCount(info.getMembers().size());
                        consumerGroupTableView.refresh();
                    });
                }
            }
        }).start();
    }

    private void showConsumerGroupDetails(String groupId) {
        if (currentCluster == null) {
            return;
        }

        new Thread(() -> {
            ConsumerGroupInfo info = ConsumerGroupService.getInstance()
                    .getConsumerGroupInfo(currentCluster.getId(), groupId);

            Platform.runLater(() -> {
                memberList.clear();
                lagList.clear();

                if (info != null) {
                    // Show members
                    for (ConsumerGroupInfo.MemberInfo member : info.getMembers()) {
                        String assignments = member.getAssignments().stream()
                                .map(ConsumerGroupInfo.TopicPartition::toString)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("");
                        
                        memberList.add(new MemberRow(
                            member.getMemberId(),
                            member.getClientId(),
                            member.getHost(),
                            assignments
                        ));
                    }

                    // Show lag information
                    info.getLag().forEach((tp, lag) -> {
                        Long offset = info.getOffsets().containsKey(tp) ? 
                                info.getOffsets().get(tp).getOffset() : 0L;
                        
                        lagList.add(new LagRow(
                            tp.getTopic(),
                            tp.getPartition(),
                            offset,
                            lag
                        ));
                    });
                }
            });
        }).start();
    }

    @FXML
    private void handleAddCluster() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(I18nUtil.get("cluster.add.title"));
        dialog.setHeaderText(I18nUtil.get("cluster.add.header"));
        dialog.setContentText(I18nUtil.get("cluster.add.name"));

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            TextInputDialog serversDialog = new TextInputDialog("localhost:9092");
            serversDialog.setTitle(I18nUtil.get("cluster.add.title"));
            serversDialog.setHeaderText(I18nUtil.get("cluster.add.header"));
            serversDialog.setContentText(I18nUtil.get("cluster.add.servers"));

            Optional<String> serversResult = serversDialog.showAndWait();
            serversResult.ifPresent(servers -> {
                ClusterConfig config = new ClusterConfig(name, servers);
                ConfigManager.getInstance().addCluster(config);
                initializeClusterTree();
                showInfo(I18nUtil.get("common.success"), I18nUtil.get("cluster.add.success", name));
            });
        });
    }

    @FXML
    private void handleRefreshTopics() {
        loadTopics();
        loadConsumerGroups();
    }

    @FXML
    private void handleSettings() {
        // Create settings dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get("settings.title"));

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("中文", "English");
        
        Locale currentLocale = I18nUtil.getCurrentLocale();
        if (currentLocale.equals(Locale.SIMPLIFIED_CHINESE)) {
            languageCombo.setValue("中文");
        } else {
            languageCombo.setValue("English");
        }

        grid.add(new Label(I18nUtil.get("settings.language")), 0, 0);
        grid.add(languageCombo, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String lang = languageCombo.getValue().equals("中文") ? "zh_CN" : "en";
            I18nUtil.setLocale(lang);
            
            // Save to config
            ConfigManager.getInstance().getConfig().getPreferences().setLanguage(lang);
            ConfigManager.getInstance().saveConfig();
            
            // Show info to restart
            showInfo(I18nUtil.get("common.info"), 
                    "Please restart the application for language changes to take effect.");
        }
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("dialog.about.title"));
        alert.setHeaderText(I18nUtil.get("dialog.about.header"));
        alert.setContentText(I18nUtil.get("dialog.about.content"));
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        if (stage != null) {
            stage.close();
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Data classes
    public static class MessageRow {
        private final Long offset;
        private final String key;
        private final String value;
        private final String timestamp;

        public MessageRow(Long offset, String key, String value, String timestamp) {
            this.offset = offset;
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }

        public Long getOffset() { return offset; }
        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getTimestamp() { return timestamp; }
    }

    public static class ConsumerGroupRow {
        private String groupId;
        private String state;
        private String coordinator;
        private int memberCount;

        public ConsumerGroupRow(String groupId, String state, String coordinator, int memberCount) {
            this.groupId = groupId;
            this.state = state;
            this.coordinator = coordinator;
            this.memberCount = memberCount;
        }

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCoordinator() { return coordinator; }
        public void setCoordinator(String coordinator) { this.coordinator = coordinator; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    }

    public static class MemberRow {
        private final String memberId;
        private final String clientId;
        private final String host;
        private final String assignments;

        public MemberRow(String memberId, String clientId, String host, String assignments) {
            this.memberId = memberId;
            this.clientId = clientId;
            this.host = host;
            this.assignments = assignments;
        }

        public String getMemberId() { return memberId; }
        public String getClientId() { return clientId; }
        public String getHost() { return host; }
        public String getAssignments() { return assignments; }
    }

    public static class LagRow {
        private final String topic;
        private final int partition;
        private final Long offset;
        private final Long lag;

        public LagRow(String topic, int partition, Long offset, Long lag) {
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.lag = lag;
        }

        public String getTopic() { return topic; }
        public int getPartition() { return partition; }
        public Long getOffset() { return offset; }
        public Long getLag() { return lag; }
    }
}
