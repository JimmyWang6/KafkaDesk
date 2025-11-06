package com.kafkadesk.ui.controller;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.core.service.ClusterService;
import com.kafkadesk.core.service.ConsumerGroupService;
import com.kafkadesk.core.service.ConsumerService;
import com.kafkadesk.core.service.ProducerService;
import com.kafkadesk.core.service.TopicService;
import com.kafkadesk.model.ClusterConfig;
import com.kafkadesk.model.ConsumerGroupInfo;
import com.kafkadesk.model.Message;
import com.kafkadesk.model.TopicInfo;
import com.kafkadesk.ui.util.I18nUtil;
import com.kafkadesk.ui.constants.I18nKeys;
import com.kafkadesk.ui.helper.ClusterOperationsHelper;
import com.kafkadesk.utils.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

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
    @FXML private Tab topicTab, producerTab, queryTab, consumerGroupsTab, configurationTab;

    // Topic Management
    @FXML private Label lblTopicList, lblTopicDetails;
    @FXML private TableView<TopicInfo> topicTableView;
    @FXML private TableColumn<TopicInfo, String> topicNameColumn;
    @FXML private TableColumn<TopicInfo, Integer> topicPartitionsColumn, topicReplicationColumn;
    @FXML private TextArea topicDetailsTextArea;
    @FXML private Button btnCreateTopic, btnDeleteTopic;

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

    // Configuration Management
    @FXML private Label lblConfigTitle, lblCurrentCluster, lblCurrentClusterValue, lblKafkaConfigs;
    @FXML private TableView<KafkaConfigRow> kafkaConfigTable;
    @FXML private TableColumn<KafkaConfigRow, String> configNameColumn, configDescColumn, configRangeColumn;
    @FXML private TableColumn<KafkaConfigRow, String> configDefaultColumn, configCurrentColumn, configOperationColumn;

    // Status bar
    @FXML private Label statusLabel;

    private Stage stage;
    private ClusterConfig currentCluster;
    private final ObservableList<TopicInfo> topicList = FXCollections.observableArrayList();
    private final ObservableList<MessageRow> messageList = FXCollections.observableArrayList();
    private final ObservableList<ConsumerGroupRow> consumerGroupList = FXCollections.observableArrayList();
    private final ObservableList<MemberRow> memberList = FXCollections.observableArrayList();
    private final ObservableList<LagRow> lagList = FXCollections.observableArrayList();
    private final ObservableList<KafkaConfigRow> kafkaConfigList = FXCollections.observableArrayList();
    
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
        initializeConfigurationView();
        initializeTabListeners();
        
        updateStatus(I18nUtil.get(I18nKeys.STATUS_READY));
    }

    /**
     * Initialize UI text with i18n
     */
    private void initializeUI() {
        // Menu
        menuFile.setText(I18nUtil.get(I18nKeys.MENU_FILE));
        menuItemAddCluster.setText(I18nUtil.get(I18nKeys.MENU_FILE_ADD_CLUSTER));
        menuItemExit.setText(I18nUtil.get(I18nKeys.MENU_FILE_EXIT));
        
        menuView.setText(I18nUtil.get(I18nKeys.MENU_VIEW));
        menuItemRefresh.setText(I18nUtil.get(I18nKeys.MENU_VIEW_REFRESH));
        
        menuTools.setText(I18nUtil.get(I18nKeys.MENU_TOOLS));
        menuItemSettings.setText(I18nUtil.get(I18nKeys.MENU_TOOLS_SETTINGS));
        
        menuHelp.setText(I18nUtil.get(I18nKeys.MENU_HELP));
        menuItemAbout.setText(I18nUtil.get(I18nKeys.MENU_HELP_ABOUT));

        // Toolbar
        btnAddCluster.setText(I18nUtil.get(I18nKeys.TOOLBAR_ADD_CLUSTER));
        btnRefresh.setText(I18nUtil.get(I18nKeys.TOOLBAR_REFRESH));
        btnSettings.setText(I18nUtil.get(I18nKeys.TOOLBAR_SETTINGS));

        // Cluster
        lblClusterList.setText(I18nUtil.get(I18nKeys.CLUSTER_LIST));

        // Tabs
        topicTab.setText(I18nUtil.get(I18nKeys.TAB_TOPICS));
        producerTab.setText(I18nUtil.get(I18nKeys.TAB_PRODUCER));
        queryTab.setText(I18nUtil.get(I18nKeys.TAB_QUERY));
        consumerGroupsTab.setText(I18nUtil.get(I18nKeys.TAB_CONSUMER_GROUPS));
        configurationTab.setText(I18nUtil.get(I18nKeys.TAB_CONFIGURATION));

        // Topic
        lblTopicList.setText(I18nUtil.get(I18nKeys.TOPIC_LIST));
        lblTopicDetails.setText(I18nUtil.get(I18nKeys.TOPIC_DETAILS));
        topicNameColumn.setText(I18nUtil.get(I18nKeys.TOPIC_NAME));
        topicPartitionsColumn.setText(I18nUtil.get(I18nKeys.TOPIC_PARTITIONS));
        topicReplicationColumn.setText(I18nUtil.get(I18nKeys.TOPIC_REPLICATION));
        btnCreateTopic.setText(I18nUtil.get(I18nKeys.TOPIC_CREATE));
        btnDeleteTopic.setText(I18nUtil.get(I18nKeys.TOPIC_DELETE));

        // Producer
        lblProducerTitle.setText(I18nUtil.get(I18nKeys.PRODUCER_TITLE));
        lblProducerTopic.setText(I18nUtil.get(I18nKeys.PRODUCER_TOPIC));
        lblProducerKey.setText(I18nUtil.get(I18nKeys.PRODUCER_KEY));
        lblProducerValue.setText(I18nUtil.get(I18nKeys.PRODUCER_VALUE));
        producerTopicField.setPromptText(I18nUtil.get(I18nKeys.PRODUCER_TOPIC_PROMPT));
        producerKeyField.setPromptText(I18nUtil.get(I18nKeys.PRODUCER_KEY_PROMPT));
        producerMessageTextArea.setPromptText(I18nUtil.get(I18nKeys.PRODUCER_VALUE_PROMPT));
        btnSendMessage.setText(I18nUtil.get(I18nKeys.PRODUCER_SEND));

        // Query
        lblQueryTopic.setText(I18nUtil.get(I18nKeys.QUERY_TOPIC));
        lblQueryPartition.setText(I18nUtil.get(I18nKeys.QUERY_PARTITION));
        lblQueryOffsetFrom.setText(I18nUtil.get(I18nKeys.QUERY_OFFSET_FROM));
        lblQueryOffsetTo.setText(I18nUtil.get(I18nKeys.QUERY_OFFSET_TO));
        lblQueryMaxRecords.setText(I18nUtil.get(I18nKeys.QUERY_MAX_RECORDS));
        lblQueryResults.setText(I18nUtil.get(I18nKeys.QUERY_RESULTS));
        lblQueryDetails.setText(I18nUtil.get(I18nKeys.QUERY_DETAILS));
        queryTopicComboBox.setPromptText(I18nUtil.get(I18nKeys.QUERY_TOPIC_PROMPT));
        btnQuerySearch.setText(I18nUtil.get(I18nKeys.QUERY_SEARCH));
        btnQueryClear.setText(I18nUtil.get(I18nKeys.QUERY_CLEAR));
        btnQueryExport.setText(I18nUtil.get(I18nKeys.QUERY_EXPORT));
        queryOffsetColumn.setText(I18nUtil.get(I18nKeys.QUERY_OFFSET));
        queryKeyColumn.setText(I18nUtil.get(I18nKeys.QUERY_KEY));
        queryValueColumn.setText(I18nUtil.get(I18nKeys.QUERY_VALUE));
        queryTimestampColumn.setText(I18nUtil.get(I18nKeys.QUERY_TIMESTAMP));

        // Consumer Groups
        lblConsumerGroupList.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LIST));
        lblConsumerGroupDetails.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_DETAILS));
        consumerGroupIdColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_ID));
        consumerGroupStateColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_STATE));
        consumerGroupCoordinatorColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_COORDINATOR));
        consumerGroupMembersColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBERS));
        
        memberIdColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBER_ID));
        memberClientIdColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBER_CLIENT_ID));
        memberHostColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBER_HOST));
        memberAssignmentsColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBER_ASSIGNMENTS));
        
        lagTopicColumn.setText(I18nUtil.get(I18nKeys.TOPIC_NAME));
        lagPartitionColumn.setText(I18nUtil.get(I18nKeys.QUERY_PARTITION));
        lagOffsetColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_OFFSET));
        lagValueColumn.setText(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LAG));
    }


    private void initializeClusterTree() {
        TreeItem<String> rootItem = new TreeItem<>(I18nUtil.get(I18nKeys.CLUSTER_LIST));
        rootItem.setExpanded(true);

        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        for (ClusterConfig cluster : clusters) {
            String displayName = cluster.getName() + " (" + cluster.getBootstrapServers() + ")";
            TreeItem<String> clusterItem = new TreeItem<>(displayName);
            clusterItem.setExpanded(false);
            rootItem.getChildren().add(clusterItem);
        }

        clusterTreeView.setRoot(rootItem);
        clusterTreeView.setShowRoot(true);

        // Add context menu for cluster items
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18nUtil.get(I18nKeys.COMMON_EDIT));
        editItem.setOnAction(e -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem != rootItem && selectedItem.getParent() == rootItem) {
                handleEditCluster(selectedItem.getValue());
            }
        });
        MenuItem deleteItem = new MenuItem(I18nUtil.get(I18nKeys.COMMON_DELETE));
        deleteItem.setOnAction(e -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem != rootItem && selectedItem.getParent() == rootItem) {
                handleDeleteCluster(selectedItem.getValue());
            }
        });
        contextMenu.getItems().addAll(editItem, deleteItem);

        // Show context menu only for cluster items (not root)
        clusterTreeView.setOnContextMenuRequested(event -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem != rootItem && selectedItem.getParent() == rootItem) {
                contextMenu.show(clusterTreeView, event.getScreenX(), event.getScreenY());
            }
        });

        clusterTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getParent() != null && newVal.getParent() == rootItem) {
                onClusterSelected(newVal.getValue());
            }
        });
    }

    private void onClusterSelected(String displayName) {
        logger.info("Cluster selected: {}", displayName);
        
        // Parse cluster name from display name
        String clusterName = ClusterOperationsHelper.parseClusterNameFromDisplay(displayName);
        
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
        updateStatus(I18nUtil.get(I18nKeys.CLUSTER_CONNECTING, config.getName()));

        new Thread(() -> {
            boolean connected = ClusterService.getInstance().connect(config);
            
            Platform.runLater(() -> {
                if (connected) {
                    updateStatus(I18nUtil.get(I18nKeys.CLUSTER_CONNECTED, config.getName()));
                    showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_CONNECTED, config.getName()));
                    loadTopics();
                    loadConsumerGroups();
                    updateQueryTopicList();
                } else {
                    updateStatus(I18nUtil.get(I18nKeys.CLUSTER_FAILED, config.getName()));
                    showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), 
                            I18nUtil.get(I18nKeys.CLUSTER_FAILED, config.getName()));
                }
            });
        }).start();
    }

    private void initializeTopicTable() {
        topicNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        topicPartitionsColumn.setCellValueFactory(new PropertyValueFactory<>("partitions"));
        topicReplicationColumn.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));

        topicTableView.setItems(topicList);
        topicTableView.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_TOPICS)));

        // Single click selection
        topicTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showTopicDetails(newVal);
            }
        });
        
        // Double click to show partition details dialog
        topicTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TopicInfo selectedTopic = topicTableView.getSelectionModel().getSelectedItem();
                if (selectedTopic != null) {
                    showPartitionDetailsDialog(selectedTopic);
                }
            }
        });
    }

    private void loadTopics() {
        if (currentCluster == null) {
            return;
        }

        updateStatus(I18nUtil.get(I18nKeys.TOPIC_LOADING));

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
                updateStatus(I18nUtil.get(I18nKeys.TOPIC_LOADED, topicNames.size()));
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
                    details.append(I18nUtil.get(I18nKeys.TOPIC_NAME)).append(": ").append(fullInfo.getName()).append("\n");
                    details.append(I18nUtil.get(I18nKeys.TOPIC_PARTITIONS)).append(": ").append(fullInfo.getPartitions()).append("\n");
                    details.append(I18nUtil.get(I18nKeys.TOPIC_REPLICATION)).append(": ").append(fullInfo.getReplicationFactor()).append("\n");
                    
                    // Get message count
                    long totalMessages = getTopicMessageCount(fullInfo.getName());
                    if (totalMessages >= 0) {
                        details.append("Total Messages: ").append(totalMessages).append("\n");
                    }
                    
                    details.append("\nConfiguration:\n");
                    fullInfo.getConfig().forEach((key, value) -> 
                        details.append("  ").append(key).append(": ").append(value).append("\n")
                    );

                    topicDetailsTextArea.setText(details.toString());
                }
            });
        }).start();
    }
    
    private long getTopicMessageCount(String topicName) {
        if (currentCluster == null) {
            return -1;
        }
        
        try {
            KafkaConsumer<String, String> consumer = ConsumerService.getInstance().createConsumer(
                currentCluster.getBootstrapServers(),
                "kafkadesk-count-" + System.currentTimeMillis()
            );
            
            TopicInfo info = TopicService.getInstance().getTopicInfo(currentCluster.getId(), topicName);
            if (info == null) {
                ConsumerService.getInstance().closeConsumer(consumer);
                return -1;
            }
            
            List<Integer> partitions = new ArrayList<>();
            for (int i = 0; i < info.getPartitions(); i++) {
                partitions.add(i);
            }
            
            ConsumerService.getInstance().assignPartitions(consumer, topicName, partitions);
            
            long totalMessages = 0;
            for (int partition : partitions) {
                TopicPartition tp = new TopicPartition(topicName, partition);
                consumer.seekToBeginning(Collections.singleton(tp));
                long beginning = consumer.position(tp);
                consumer.seekToEnd(Collections.singleton(tp));
                long end = consumer.position(tp);
                totalMessages += (end - beginning);
            }
            
            ConsumerService.getInstance().closeConsumer(consumer);
            return totalMessages;
        } catch (Exception e) {
            logger.error("Failed to get message count for topic: " + topicName, e);
            return -1;
        }
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
            showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.PRODUCER_ERROR_REQUIRED));
            return;
        }

        if (currentCluster == null) {
            showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), I18nUtil.get(I18nKeys.QUERY_NO_CONNECTION));
            return;
        }

        updateStatus(I18nUtil.get(I18nKeys.PRODUCER_SENDING));

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
                    updateStatus(I18nUtil.get(I18nKeys.PRODUCER_SUCCESS));
                    showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.PRODUCER_SUCCESS));
                    producerMessageTextArea.clear();
                });
            } catch (Exception e) {
                logger.error("Failed to send message", e);
                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get(I18nKeys.PRODUCER_FAILED, e.getMessage()));
                    showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), 
                            I18nUtil.get(I18nKeys.PRODUCER_FAILED, e.getMessage()));
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
        queryTableView.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_MESSAGES)));
        
        queryTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showMessageDetails(newVal);
            }
        });
        
        // Make topic combo box editable for easier selection
        queryTopicComboBox.setEditable(true);
        
        // Add listener for topic selection to update partitions
        queryTopicComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                updatePartitionList(newVal);
            }
        });
        
        queryPartitionComboBox.getItems().add(I18nUtil.get(I18nKeys.QUERY_PARTITION_ALL));
        queryPartitionComboBox.setValue(I18nUtil.get(I18nKeys.QUERY_PARTITION_ALL));
    }

    private void updateQueryTopicList() {
        if (currentCluster == null) {
            return;
        }

        String previousSelection = queryTopicComboBox.getValue();
        queryTopicComboBox.getItems().clear();
        queryTopicComboBox.getItems().addAll(
            TopicService.getInstance().listTopics(currentCluster.getId())
        );
        
        // Restore previous selection if it still exists
        if (previousSelection != null && queryTopicComboBox.getItems().contains(previousSelection)) {
            queryTopicComboBox.setValue(previousSelection);
        }
    }

    private void updatePartitionList(String topic) {
        queryPartitionComboBox.getItems().clear();
        queryPartitionComboBox.getItems().add(I18nUtil.get(I18nKeys.QUERY_PARTITION_ALL));
        
        TopicInfo info = TopicService.getInstance().getTopicInfo(currentCluster.getId(), topic);
        if (info != null) {
            for (int i = 0; i < info.getPartitions(); i++) {
                queryPartitionComboBox.getItems().add(String.valueOf(i));
            }
        }
        queryPartitionComboBox.setValue(I18nUtil.get(I18nKeys.QUERY_PARTITION_ALL));
    }

    @FXML
    private void handleQueryMessages() {
        String topic = queryTopicComboBox.getValue();
        if (topic == null || topic.isEmpty()) {
            showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), I18nUtil.get(I18nKeys.PRODUCER_ERROR_REQUIRED));
            return;
        }

        if (currentCluster == null) {
            showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), I18nUtil.get(I18nKeys.QUERY_NO_CONNECTION));
            return;
        }

        messageList.clear();
        updateStatus(I18nUtil.get(I18nKeys.QUERY_SEARCHING));

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
                if (partitionStr.equals(I18nUtil.get(I18nKeys.QUERY_PARTITION_ALL))) {
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
                    updateStatus(I18nUtil.get(I18nKeys.QUERY_FOUND, finalCount));
                });

                ConsumerService.getInstance().closeConsumer(queryConsumer);
                queryConsumer = null;

            } catch (Exception e) {
                logger.error("Failed to query messages", e);
                Platform.runLater(() -> {
                    updateStatus(I18nUtil.get(I18nKeys.ERROR_QUERY_FAILED));
                    showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), 
                            I18nUtil.get(I18nKeys.ERROR_QUERY_FAILED) + ": " + e.getMessage());
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
        showInfo(I18nUtil.get(I18nKeys.COMMON_INFO), "Export functionality coming soon");
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
        consumerGroupTableView.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_CONSUMER_GROUPS)));

        memberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberClientIdColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        memberHostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        memberAssignmentsColumn.setCellValueFactory(new PropertyValueFactory<>("assignments"));

        consumerGroupMembersTableView.setItems(memberList);
        consumerGroupMembersTableView.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_DATA)));

        lagTopicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        lagPartitionColumn.setCellValueFactory(new PropertyValueFactory<>("partition"));
        lagOffsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        lagValueColumn.setCellValueFactory(new PropertyValueFactory<>("lag"));

        consumerGroupLagTableView.setItems(lagList);
        consumerGroupLagTableView.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_DATA)));

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

        updateStatus(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LOADING));

        new Thread(() -> {
            List<String> groupIds = ConsumerGroupService.getInstance().listConsumerGroups(currentCluster.getId());
            
            Platform.runLater(() -> {
                consumerGroupList.clear();
                for (String groupId : groupIds) {
                    ConsumerGroupRow row = new ConsumerGroupRow(groupId, "", "", 0);
                    consumerGroupList.add(row);
                }
                updateStatus(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LOADED, groupIds.size()));
                
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
        // Create add cluster dialog similar to edit dialog
        Dialog<ClusterConfig> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.CLUSTER_ADD_TITLE));
        dialog.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_ADD_HEADER));
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        TextField hostnameField = new TextField("localhost");
        TextField portField = new TextField("9092");
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL");
        protocolCombo.setValue("PLAINTEXT");
        
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_ADD_NAME)), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_HOST)), 0, 1);
        grid.add(hostnameField, 1, 1);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_PORT)), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_PROTOCOL)), 0, 3);
        grid.add(protocolCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Add validation
        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_NAME_EMPTY));
                event.consume();
            } else if (hostnameField.getText() == null || hostnameField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_HOST_EMPTY));
                event.consume();
            } else if (portField.getText() == null || portField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_PORT_EMPTY));
                event.consume();
            } else {
                // Validate port is a number
                try {
                    Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException e) {
                    showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_PORT_INVALID));
                    event.consume();
                }
            }
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ClusterConfig config = new ClusterConfig();
                config.setName(nameField.getText().trim());
                config.setBootstrapServers(hostnameField.getText().trim() + ":" + portField.getText().trim());
                config.setSecurityProtocol(protocolCombo.getValue());
                return config;
            }
            return null;
        });
        
        Optional<ClusterConfig> result = dialog.showAndWait();
        result.ifPresent(config -> {
            ConfigManager.getInstance().addCluster(config);
            initializeClusterTree();
            showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_ADD_SUCCESS, config.getName()));
        });
    }

    private void handleDeleteCluster(String displayName) {
        // Parse cluster name from display name
        String clusterName = ClusterOperationsHelper.parseClusterNameFromDisplay(displayName);
        
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtil.get(I18nKeys.CLUSTER_DELETE_TITLE));
        alert.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_DELETE_CONFIRM, clusterName));
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Find and delete the cluster
            List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
            Optional<ClusterConfig> cluster = clusters.stream()
                    .filter(c -> c.getName().equals(clusterName))
                    .findFirst();
            
            if (cluster.isPresent()) {
                ConfigManager.getInstance().deleteCluster(cluster.get().getId());
                
                // Clear current cluster if it was deleted
                if (currentCluster != null && currentCluster.getId().equals(cluster.get().getId())) {
                    currentCluster = null;
                    topicList.clear();
                    consumerGroupList.clear();
                    messageList.clear();
                }
                
                initializeClusterTree();
                showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_DELETE_SUCCESS, clusterName));
            }
        }
    }

    private void handleEditCluster(String displayName) {
        // Parse cluster name from display name
        String clusterName = ClusterOperationsHelper.parseClusterNameFromDisplay(displayName);
        
        // Find the cluster
        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        Optional<ClusterConfig> clusterOpt = clusters.stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst();
        
        if (!clusterOpt.isPresent()) {
            return;
        }
        
        ClusterConfig cluster = clusterOpt.get();
        
        // Create edit dialog
        Dialog<ClusterConfig> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.CLUSTER_EDIT_TITLE));
        dialog.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_EDIT_HEADER));
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(cluster.getName());
        TextField hostnameField = new TextField();
        TextField portField = new TextField();
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL");
        
        // Parse bootstrap servers to get hostname and port
        String bootstrapServers = cluster.getBootstrapServers();
        if (bootstrapServers != null && bootstrapServers.contains(":")) {
            String[] parts = bootstrapServers.split(":");
            hostnameField.setText(parts[0]);
            if (parts.length > 1) {
                portField.setText(parts[1].split(",")[0]); // Get first port if multiple servers
            }
        } else {
            hostnameField.setText(bootstrapServers != null ? bootstrapServers : "localhost");
            portField.setText("9092");
        }
        
        // Set protocol
        String protocol = cluster.getSecurityProtocol();
        if (protocol != null && !protocol.isEmpty()) {
            protocolCombo.setValue(protocol);
        } else {
            protocolCombo.setValue("PLAINTEXT");
        }
        
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_ADD_NAME)), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_HOST)), 0, 1);
        grid.add(hostnameField, 1, 1);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_PORT)), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label(I18nUtil.get(I18nKeys.CLUSTER_EDIT_PROTOCOL)), 0, 3);
        grid.add(protocolCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Add validation
        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_NAME_EMPTY));
                event.consume();
            } else if (hostnameField.getText() == null || hostnameField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_HOST_EMPTY));
                event.consume();
            } else if (portField.getText() == null || portField.getText().trim().isEmpty()) {
                showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_PORT_EMPTY));
                event.consume();
            } else {
                // Validate port is a number
                try {
                    Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException e) {
                    showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), I18nUtil.get(I18nKeys.CLUSTER_EDIT_ERROR_PORT_INVALID));
                    event.consume();
                }
            }
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ClusterConfig updatedCluster = new ClusterConfig();
                updatedCluster.setId(cluster.getId());
                updatedCluster.setName(nameField.getText().trim());
                updatedCluster.setBootstrapServers(hostnameField.getText().trim() + ":" + portField.getText().trim());
                updatedCluster.setSecurityProtocol(protocolCombo.getValue());
                updatedCluster.setCreatedAt(cluster.getCreatedAt());
                return updatedCluster;
            }
            return null;
        });
        
        Optional<ClusterConfig> result = dialog.showAndWait();
        result.ifPresent(updatedCluster -> {
            ConfigManager.getInstance().updateCluster(updatedCluster);
            
            // Update current cluster if it was edited
            if (currentCluster != null && currentCluster.getId().equals(updatedCluster.getId())) {
                currentCluster = updatedCluster;
            }
            
            initializeClusterTree();
            showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_EDIT_SUCCESS, updatedCluster.getName()));
        });
    }

    @FXML
    private void handleCreateTopic() {
        if (currentCluster == null) {
            showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Please select a cluster first");
            return;
        }

        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.TOPIC_CREATE_TITLE));
        dialog.setHeaderText(I18nUtil.get(I18nKeys.TOPIC_CREATE_HEADER));

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Topic name");
        TextField partitionsField = new TextField();
        partitionsField.setPromptText("Number of partitions");
        partitionsField.setText("1");
        TextField replicationField = new TextField();
        replicationField.setPromptText("Replication factor");
        replicationField.setText("1");

        grid.add(new Label(I18nUtil.get(I18nKeys.TOPIC_NAME) + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18nUtil.get(I18nKeys.TOPIC_PARTITIONS) + ":"), 0, 1);
        grid.add(partitionsField, 1, 1);
        grid.add(new Label(I18nUtil.get(I18nKeys.TOPIC_REPLICATION) + ":"), 0, 2);
        grid.add(replicationField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String topicName = nameField.getText().trim();
            String partitionsStr = partitionsField.getText().trim();
            String replicationStr = replicationField.getText().trim();

            // Validation
            if (topicName.isEmpty()) {
                showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Topic name cannot be empty");
                return;
            }

            try {
                int partitions = Integer.parseInt(partitionsStr);
                short replication = Short.parseShort(replicationStr);

                if (partitions < 1) {
                    showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Partitions must be at least 1");
                    return;
                }

                if (replication < 1) {
                    showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Replication factor must be at least 1");
                    return;
                }

                // Create topic
                boolean success = TopicService.getInstance().createTopic(currentCluster.getId(), topicName, partitions, replication);

                if (success) {
                    showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.TOPIC_CREATE_SUCCESS));
                    loadTopics();
                } else {
                    showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_CREATE_ERROR));
                }
            } catch (NumberFormatException e) {
                showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Invalid number format");
            }
        }
    }

    @FXML
    private void handleDeleteTopic() {
        if (currentCluster == null) {
            showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Please select a cluster first");
            return;
        }

        TopicInfo selectedTopic = topicTableView.getSelectionModel().getSelectedItem();
        if (selectedTopic == null) {
            showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_DELETE_NO_SELECTION));
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(I18nUtil.get(I18nKeys.TOPIC_DELETE_TITLE));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(I18nUtil.get(I18nKeys.TOPIC_DELETE_CONFIRM, selectedTopic.getName()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = TopicService.getInstance().deleteTopic(currentCluster.getId(), selectedTopic.getName());

            if (success) {
                showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.TOPIC_DELETE_SUCCESS));
                loadTopics();
            } else {
                showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_DELETE_ERROR));
            }
        }
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
        dialog.setTitle(I18nUtil.get(I18nKeys.SETTINGS_TITLE));

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

        grid.add(new Label(I18nUtil.get(I18nKeys.SETTINGS_LANGUAGE)), 0, 0);
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
            showInfo(I18nUtil.get(I18nKeys.COMMON_INFO), 
                    "Please restart the application for language changes to take effect.");
        }
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get(I18nKeys.DIALOG_ABOUT_TITLE));
        alert.setHeaderText(I18nUtil.get(I18nKeys.DIALOG_ABOUT_HEADER));
        alert.setContentText(I18nUtil.get(I18nKeys.DIALOG_ABOUT_CONTENT));
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        if (stage != null) {
            stage.close();
        }
    }

    private void initializeTabListeners() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && currentCluster != null) {
                if (newTab == topicTab) {
                    loadTopics();
                } else if (newTab == consumerGroupsTab) {
                    loadConsumerGroups();
                } else if (newTab == queryTab) {
                    updateQueryTopicList();
                } else if (newTab == configurationTab) {
                    loadConfigurationView();
                }
            }
        });
    }

    private void initializeConfigurationView() {
        // Set labels
        lblConfigTitle.setText(I18nUtil.get(I18nKeys.CONFIG_TITLE));
        lblCurrentCluster.setText(I18nUtil.get(I18nKeys.CONFIG_CURRENT_CLUSTER));
        lblKafkaConfigs.setText(I18nUtil.get(I18nKeys.CONFIG_PROPERTIES));
        
        // Setup kafka config table columns
        configNameColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_NAME));
        configDescColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_DESC));
        configRangeColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_RANGE));
        configDefaultColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_DEFAULT));
        configCurrentColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_CURRENT));
        configOperationColumn.setText(I18nUtil.get(I18nKeys.CONFIG_PARAM_OPERATION));
        
        configNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        configDescColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        configRangeColumn.setCellValueFactory(new PropertyValueFactory<>("range"));
        configDefaultColumn.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
        configCurrentColumn.setCellValueFactory(new PropertyValueFactory<>("currentValue"));
        
        // Add edit button to operation column
        configOperationColumn.setCellFactory(col -> new javafx.scene.control.TableCell<KafkaConfigRow, String>() {
            private final Button editButton = new Button(I18nUtil.get(I18nKeys.CONFIG_PARAM_EDIT));
            
            {
                editButton.setOnAction(event -> {
                    KafkaConfigRow config = getTableView().getItems().get(getIndex());
                    handleEditKafkaConfig(config);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editButton);
                }
            }
        });
        
        kafkaConfigTable.setItems(kafkaConfigList);
        kafkaConfigTable.setPlaceholder(new Label(I18nUtil.get(I18nKeys.PLACEHOLDER_NO_DATA)));
        
        // Set initial state
        lblCurrentClusterValue.setText(I18nUtil.get(I18nKeys.CONFIG_NO_CLUSTER_SELECTED));
    }

    private void loadConfigurationView() {
        if (currentCluster == null) {
            lblCurrentClusterValue.setText(I18nUtil.get(I18nKeys.CONFIG_NO_CLUSTER_SELECTED));
            kafkaConfigList.clear();
            return;
        }
        
        // Load current cluster configuration
        lblCurrentClusterValue.setText(currentCluster.getName());
        
        // Load Kafka broker configurations
        kafkaConfigList.clear();
        loadKafkaConfigurations();
    }

    private void loadKafkaConfigurations() {
        // Add common Kafka broker configurations
        // These are example configurations - in a real implementation, 
        // you would fetch these from the Kafka cluster
        
        kafkaConfigList.add(new KafkaConfigRow(
            "min.insync.replicas",
            I18nUtil.get(I18nKeys.CONFIG_DESC_MIN_INSYNC_REPLICAS),
            ">=1",
            "1",
            currentCluster.getProperties().getOrDefault("min.insync.replicas", "1")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "unclean.leader.election.enable",
            I18nUtil.get(I18nKeys.CONFIG_DESC_UNCLEAN_LEADER_ELECTION),
            "true/false",
            "false",
            currentCluster.getProperties().getOrDefault("unclean.leader.election.enable", "false")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "log.retention.hours",
            I18nUtil.get(I18nKeys.CONFIG_DESC_LOG_RETENTION_HOURS),
            ">=1",
            "168",
            currentCluster.getProperties().getOrDefault("log.retention.hours", "168")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "log.retention.bytes",
            I18nUtil.get(I18nKeys.CONFIG_DESC_LOG_RETENTION_BYTES),
            ">=-1",
            "-1",
            currentCluster.getProperties().getOrDefault("log.retention.bytes", "-1")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "log.segment.bytes",
            I18nUtil.get(I18nKeys.CONFIG_DESC_LOG_SEGMENT_BYTES),
            ">=14",
            "1073741824",
            currentCluster.getProperties().getOrDefault("log.segment.bytes", "1073741824")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "compression.type",
            I18nUtil.get(I18nKeys.CONFIG_DESC_COMPRESSION_TYPE),
            "none,gzip,snappy,lz4,zstd",
            "producer",
            currentCluster.getProperties().getOrDefault("compression.type", "producer")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "num.partitions",
            I18nUtil.get(I18nKeys.CONFIG_DESC_NUM_PARTITIONS),
            ">=1",
            "1",
            currentCluster.getProperties().getOrDefault("num.partitions", "1")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "default.replication.factor",
            I18nUtil.get(I18nKeys.CONFIG_DESC_DEFAULT_REPLICATION_FACTOR),
            ">=1",
            "1",
            currentCluster.getProperties().getOrDefault("default.replication.factor", "1")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "max.message.bytes",
            I18nUtil.get(I18nKeys.CONFIG_DESC_MAX_MESSAGE_BYTES),
            ">=0",
            "1048588",
            currentCluster.getProperties().getOrDefault("max.message.bytes", "1048588")
        ));
        
        kafkaConfigList.add(new KafkaConfigRow(
            "replica.lag.time.max.ms",
            I18nUtil.get(I18nKeys.CONFIG_DESC_REPLICA_LAG_TIME_MAX_MS),
            ">=0",
            "30000",
            currentCluster.getProperties().getOrDefault("replica.lag.time.max.ms", "30000")
        ));
    }

    private void handleEditKafkaConfig(KafkaConfigRow config) {
        TextInputDialog dialog = new TextInputDialog(config.getCurrentValue());
        dialog.setTitle(I18nUtil.get(I18nKeys.CONFIG_PARAM_EDIT));
        dialog.setHeaderText(config.getName());
        dialog.setContentText(config.getDescription() + "\n" + 
                             I18nUtil.get(I18nKeys.CONFIG_PARAM_RANGE) + ": " + config.getRange() + "\n" +
                             I18nUtil.get(I18nKeys.CONFIG_PARAM_DEFAULT) + ": " + config.getDefaultValue());
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newValue -> {
            // Update the configuration
            config.setCurrentValue(newValue);
            
            // Update in cluster config
            if (currentCluster != null) {
                java.util.Map<String, String> properties = currentCluster.getProperties();
                if (properties == null) {
                    properties = new java.util.HashMap<>();
                    currentCluster.setProperties(properties);
                }
                properties.put(config.getName(), newValue);
                
                // Save to config manager
                ConfigManager.getInstance().updateCluster(currentCluster);
                
                // Refresh table
                kafkaConfigTable.refresh();
                
                showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), 
                        "Configuration updated: " + config.getName() + " = " + newValue);
            }
        });
    }

    private void showPartitionDetailsDialog(TopicInfo topic) {
        if (currentCluster == null) {
            return;
        }

        new Thread(() -> {
            TopicInfo fullInfo = TopicService.getInstance().getTopicInfo(currentCluster.getId(), topic.getName());
            
            Platform.runLater(() -> {
                if (fullInfo == null || fullInfo.getPartitionDetails().isEmpty()) {
                    showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), "Failed to load partition details");
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nUtil.get(I18nKeys.TOPIC_DETAILS) + " - " + topic.getName());
                alert.setHeaderText("Partition Details");
                
                StringBuilder details = new StringBuilder();
                details.append(String.format("Topic: %s\n", fullInfo.getName()));
                details.append(String.format("Partitions: %d\n", fullInfo.getPartitions()));
                details.append(String.format("Replication Factor: %d\n\n", fullInfo.getReplicationFactor()));
                details.append("Partition Information:\n");
                details.append("─".repeat(60)).append("\n");
                
                for (TopicInfo.PartitionInfo partitionInfo : fullInfo.getPartitionDetails()) {
                    details.append(String.format("\nPartition %d:\n", partitionInfo.getPartition()));
                    if (partitionInfo.getLeader() != null) {
                        details.append(String.format("  Leader: %s\n", partitionInfo.getLeader()));
                    }
                    details.append(String.format("  Replicas: %s\n", 
                        partitionInfo.getReplicas().stream()
                            .map(TopicInfo.Node::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("None")));
                    details.append(String.format("  ISR: %s\n", 
                        partitionInfo.getIsr().stream()
                            .map(TopicInfo.Node::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("None")));
                }
                
                TextArea textArea = new TextArea(details.toString());
                textArea.setEditable(false);
                textArea.setPrefRowCount(20);
                textArea.setPrefColumnCount(60);
                
                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
            });
        }).start();
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

    public static class KafkaConfigRow {
        private final String name;
        private final String description;
        private final String range;
        private final String defaultValue;
        private String currentValue;

        public KafkaConfigRow(String name, String description, String range, String defaultValue, String currentValue) {
            this.name = name;
            this.description = description;
            this.range = range;
            this.defaultValue = defaultValue;
            this.currentValue = currentValue;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getRange() { return range; }
        public String getDefaultValue() { return defaultValue; }
        public String getCurrentValue() { return currentValue; }
        public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }
    }
}
