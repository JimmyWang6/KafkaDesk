package com.kafkadesk.controller;

import com.kafkadesk.model.*;
import com.kafkadesk.service.*;
import com.kafkadesk.util.ConnectionPersistence;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Main controller for the KafkaDesk application
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Services
    private final KafkaConnectionService connectionService = new KafkaConnectionService();
    private final TopicService topicService = new TopicService(connectionService);
    private final MessageService messageService = new MessageService(connectionService);
    private final ConsumerGroupService consumerGroupService = new ConsumerGroupService(connectionService);
    private final MonitoringService monitoringService = new MonitoringService(connectionService);
    private final ConnectionPersistence persistence = new ConnectionPersistence();

    // Data
    private final ObservableList<KafkaConnection> connections = FXCollections.observableArrayList();
    private final ObservableList<TopicInfo> topics = FXCollections.observableArrayList();
    private final ObservableList<KafkaMessage> messages = FXCollections.observableArrayList();
    private final ObservableList<ConsumerGroupInfo> consumerGroups = FXCollections.observableArrayList();

    private KafkaConnection currentConnection;

    // Connection UI
    @FXML private ListView<KafkaConnection> connectionListView;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private Button addConnectionButton;
    @FXML private Button deleteConnectionButton;

    // Topic UI
    @FXML private TableView<TopicInfo> topicTableView;
    @FXML private TableColumn<TopicInfo, String> topicNameColumn;
    @FXML private TableColumn<TopicInfo, Integer> topicPartitionsColumn;
    @FXML private TableColumn<TopicInfo, Short> topicReplicationColumn;
    @FXML private Button refreshTopicsButton;
    @FXML private Button createTopicButton;
    @FXML private Button deleteTopicButton;

    // Message UI
    @FXML private TableView<KafkaMessage> messageTableView;
    @FXML private TableColumn<KafkaMessage, Integer> messagePartitionColumn;
    @FXML private TableColumn<KafkaMessage, Long> messageOffsetColumn;
    @FXML private TableColumn<KafkaMessage, String> messageKeyColumn;
    @FXML private TableColumn<KafkaMessage, String> messageValueColumn;
    @FXML private Button browseMessagesButton;
    @FXML private Button sendMessageButton;
    @FXML private TextField searchTextField;
    @FXML private Button searchButton;

    // Consumer Group UI
    @FXML private TableView<ConsumerGroupInfo> consumerGroupTableView;
    @FXML private TableColumn<ConsumerGroupInfo, String> groupIdColumn;
    @FXML private TableColumn<ConsumerGroupInfo, String> groupStateColumn;
    @FXML private TableColumn<ConsumerGroupInfo, Integer> groupMembersColumn;
    @FXML private TableColumn<ConsumerGroupInfo, Long> groupLagColumn;
    @FXML private Button refreshConsumerGroupsButton;

    // Status bar
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        setupConnectionListView();
        setupTopicTableView();
        setupMessageTableView();
        setupConsumerGroupTableView();
        loadConnections();
        updateUIState();
    }

    private void setupConnectionListView() {
        connectionListView.setItems(connections);
        connectionListView.setCellFactory(lv -> new ListCell<KafkaConnection>() {
            @Override
            protected void updateItem(KafkaConnection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getBootstrapServers());
                }
            }
        });
    }

    private void setupTopicTableView() {
        topicNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        topicPartitionsColumn.setCellValueFactory(new PropertyValueFactory<>("partitions"));
        topicReplicationColumn.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));
        topicTableView.setItems(topics);
    }

    private void setupMessageTableView() {
        messagePartitionColumn.setCellValueFactory(new PropertyValueFactory<>("partition"));
        messageOffsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        messageKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        messageValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        messageTableView.setItems(messages);
    }

    private void setupConsumerGroupTableView() {
        groupIdColumn.setCellValueFactory(new PropertyValueFactory<>("groupId"));
        groupStateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        groupMembersColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
        groupLagColumn.setCellValueFactory(new PropertyValueFactory<>("lag"));
        consumerGroupTableView.setItems(consumerGroups);
    }

    @FXML
    private void handleConnect() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a connection");
            return;
        }

        new Thread(() -> {
            try {
                connectionService.connect(selected);
                Platform.runLater(() -> {
                    currentConnection = selected;
                    updateStatus("Connected to " + selected.getName());
                    updateUIState();
                    loadTopics();
                    loadConsumerGroups();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to connect: " + e.getMessage()));
                logger.error("Connection failed", e);
            }
        }).start();
    }

    @FXML
    private void handleDisconnect() {
        if (currentConnection != null) {
            connectionService.disconnect(currentConnection.getId());
            currentConnection = null;
            topics.clear();
            messages.clear();
            consumerGroups.clear();
            updateStatus("Disconnected");
            updateUIState();
        }
    }

    @FXML
    private void handleAddConnection() {
        Dialog<KafkaConnection> dialog = createConnectionDialog(null);
        Optional<KafkaConnection> result = dialog.showAndWait();
        
        result.ifPresent(connection -> {
            persistence.saveConnection(connection, connections);
            connections.add(connection);
            updateStatus("Connection added: " + connection.getName());
        });
    }

    @FXML
    private void handleDeleteConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a connection to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Connection");
        alert.setHeaderText("Delete connection: " + selected.getName() + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (currentConnection != null && currentConnection.equals(selected)) {
                handleDisconnect();
            }
            persistence.deleteConnection(selected.getId(), connections);
            connections.remove(selected);
            updateStatus("Connection deleted");
        }
    }

    @FXML
    private void handleRefreshTopics() {
        loadTopics();
    }

    @FXML
    private void handleCreateTopic() {
        if (currentConnection == null) {
            showError("Please connect to a cluster first");
            return;
        }

        Dialog<TopicInfo> dialog = createTopicDialog();
        Optional<TopicInfo> result = dialog.showAndWait();
        
        result.ifPresent(topic -> {
            new Thread(() -> {
                try {
                    topicService.createTopic(currentConnection.getId(), topic.getName(), 
                                            topic.getPartitions(), topic.getReplicationFactor());
                    Platform.runLater(() -> {
                        updateStatus("Topic created: " + topic.getName());
                        loadTopics();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to create topic: " + e.getMessage()));
                    logger.error("Topic creation failed", e);
                }
            }).start();
        });
    }

    @FXML
    private void handleDeleteTopic() {
        if (currentConnection == null) {
            showError("Please connect to a cluster first");
            return;
        }

        TopicInfo selected = topicTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a topic to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Topic");
        alert.setHeaderText("Delete topic: " + selected.getName() + "?");
        alert.setContentText("This action cannot be undone and all data will be lost.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    topicService.deleteTopic(currentConnection.getId(), selected.getName());
                    Platform.runLater(() -> {
                        updateStatus("Topic deleted: " + selected.getName());
                        loadTopics();
                        messages.clear();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to delete topic: " + e.getMessage()));
                    logger.error("Topic deletion failed", e);
                }
            }).start();
        }
    }

    @FXML
    private void handleBrowseMessages() {
        if (currentConnection == null) {
            showError("Please connect to a cluster first");
            return;
        }

        TopicInfo selected = topicTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a topic");
            return;
        }

        new Thread(() -> {
            try {
                List<KafkaMessage> msgs = messageService.browseMessages(currentConnection, 
                                                                        selected.getName(), 100);
                Platform.runLater(() -> {
                    messages.setAll(msgs);
                    updateStatus("Loaded " + msgs.size() + " messages from " + selected.getName());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to browse messages: " + e.getMessage()));
                logger.error("Message browsing failed", e);
            }
        }).start();
    }

    @FXML
    private void handleSendMessage() {
        if (currentConnection == null) {
            showError("Please connect to a cluster first");
            return;
        }

        TopicInfo selected = topicTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a topic");
            return;
        }

        Dialog<KafkaMessage> dialog = createMessageDialog(selected.getName());
        Optional<KafkaMessage> result = dialog.showAndWait();
        
        result.ifPresent(message -> {
            new Thread(() -> {
                try {
                    messageService.sendMessage(currentConnection, selected.getName(), 
                                              message.getKey(), message.getValue());
                    Platform.runLater(() -> updateStatus("Message sent to " + selected.getName()));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to send message: " + e.getMessage()));
                    logger.error("Message sending failed", e);
                }
            }).start();
        });
    }

    @FXML
    private void handleSearch() {
        if (currentConnection == null) {
            showError("Please connect to a cluster first");
            return;
        }

        TopicInfo selected = topicTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a topic");
            return;
        }

        String searchTerm = searchTextField.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            showError("Please enter a search term");
            return;
        }

        new Thread(() -> {
            try {
                List<KafkaMessage> msgs = messageService.searchMessages(currentConnection, 
                                                                        selected.getName(), 
                                                                        searchTerm.trim(), 50);
                Platform.runLater(() -> {
                    messages.setAll(msgs);
                    updateStatus("Found " + msgs.size() + " matching messages");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to search messages: " + e.getMessage()));
                logger.error("Message search failed", e);
            }
        }).start();
    }

    @FXML
    private void handleRefreshConsumerGroups() {
        loadConsumerGroups();
    }

    private void loadConnections() {
        List<KafkaConnection> loaded = persistence.loadConnections();
        connections.setAll(loaded);
    }

    private void loadTopics() {
        if (currentConnection == null) {
            return;
        }

        new Thread(() -> {
            try {
                List<TopicInfo> topicList = topicService.listTopics(currentConnection.getId());
                Platform.runLater(() -> {
                    topics.setAll(topicList);
                    updateStatus("Loaded " + topicList.size() + " topics");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load topics: " + e.getMessage()));
                logger.error("Failed to load topics", e);
            }
        }).start();
    }

    private void loadConsumerGroups() {
        if (currentConnection == null) {
            return;
        }

        new Thread(() -> {
            try {
                List<ConsumerGroupInfo> groups = consumerGroupService.listConsumerGroups(
                        currentConnection.getId());
                Platform.runLater(() -> {
                    consumerGroups.setAll(groups);
                    updateStatus("Loaded " + groups.size() + " consumer groups");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load consumer groups: " + e.getMessage()));
                logger.error("Failed to load consumer groups", e);
            }
        }).start();
    }

    private Dialog<KafkaConnection> createConnectionDialog(KafkaConnection existing) {
        Dialog<KafkaConnection> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Connection" : "Edit Connection");
        dialog.setHeaderText("Enter Kafka connection details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField serversField = new TextField(existing != null ? existing.getBootstrapServers() : "");
        TextField descField = new TextField(existing != null ? existing.getDescription() : "");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Bootstrap Servers:"), 0, 1);
        grid.add(serversField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                KafkaConnection conn = existing != null ? existing : new KafkaConnection();
                conn.setName(nameField.getText());
                conn.setBootstrapServers(serversField.getText());
                conn.setDescription(descField.getText());
                return conn;
            }
            return null;
        });

        return dialog;
    }

    private Dialog<TopicInfo> createTopicDialog() {
        Dialog<TopicInfo> dialog = new Dialog<>();
        dialog.setTitle("Create Topic");
        dialog.setHeaderText("Enter topic details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField partitionsField = new TextField("3");
        TextField replicationField = new TextField("1");

        grid.add(new Label("Topic Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Partitions:"), 0, 1);
        grid.add(partitionsField, 1, 1);
        grid.add(new Label("Replication Factor:"), 0, 2);
        grid.add(replicationField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                TopicInfo topic = new TopicInfo();
                topic.setName(nameField.getText());
                topic.setPartitions(Integer.parseInt(partitionsField.getText()));
                topic.setReplicationFactor(Short.parseShort(replicationField.getText()));
                return topic;
            }
            return null;
        });

        return dialog;
    }

    private Dialog<KafkaMessage> createMessageDialog(String topicName) {
        Dialog<KafkaMessage> dialog = new Dialog<>();
        dialog.setTitle("Send Message");
        dialog.setHeaderText("Send message to topic: " + topicName);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField keyField = new TextField();
        TextArea valueArea = new TextArea();
        valueArea.setPrefRowCount(5);

        grid.add(new Label("Key (optional):"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                KafkaMessage msg = new KafkaMessage();
                msg.setKey(keyField.getText());
                msg.setValue(valueArea.getText());
                return msg;
            }
            return null;
        });

        return dialog;
    }

    private void updateUIState() {
        boolean connected = currentConnection != null;
        
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        
        refreshTopicsButton.setDisable(!connected);
        createTopicButton.setDisable(!connected);
        deleteTopicButton.setDisable(!connected);
        
        browseMessagesButton.setDisable(!connected);
        sendMessageButton.setDisable(!connected);
        searchButton.setDisable(!connected);
        
        refreshConsumerGroupsButton.setDisable(!connected);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        logger.info(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        connectionService.closeAll();
    }
}
