package com.kafkadesk.ui.controller;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.core.service.ClusterService;
import com.kafkadesk.core.service.ConsumerGroupService;
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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 * Main Window Controller with TreeView-based cluster navigation
 */
public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Menu components
    @FXML private MenuBar menuBar;
    @FXML private Menu menuFile, menuView, menuTools, menuHelp;
    @FXML private MenuItem menuItemAddCluster, menuItemExit, menuItemRefresh, menuItemSettings, menuItemAbout;
    
    // Toolbar components
    @FXML private Button btnAddCluster, btnRefresh;
    
    // Cluster tree (left side)
    // @FXML private Label lblClusterList;  // Removed - buttons now replace the label
    @FXML private TreeView<String> clusterTreeView;

    // Content area (right side)
    @FXML private StackPane contentArea;

    // Status bar
    @FXML private Label statusLabel;

    private Stage stage;
    private final Map<String, ClusterContentManager> clusterContentManagers = new HashMap<>();
    private final Map<String, TreeItem<String>> clusterTreeItems = new HashMap<>();
    private final Map<TreeItem<String>, TreeItemData> treeItemDataMap = new HashMap<>();
    
    // Tree item types
    private static final String TYPE_ROOT = "ROOT";
    private static final String TYPE_CLUSTER = "CLUSTER";
    private static final String TYPE_OVERVIEW = "OVERVIEW";
    private static final String TYPE_BROKERS = "BROKERS";
    private static final String TYPE_TOPICS = "TOPICS";
    private static final String TYPE_CONSUMER_GROUPS = "CONSUMER_GROUPS";
    private static final String TYPE_ACL = "ACL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController with TreeView-based cluster navigation");
        
        // Initialize i18n from configuration
        String language = ConfigManager.getInstance().getConfig().getPreferences().getLanguage();
        I18nUtil.setLocale(language);

        initializeUI();
        initializeClusterTree();
        
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

        // Toolbar with icons only
        btnAddCluster.setText("➕");
        btnAddCluster.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOOLBAR_ADD_CLUSTER)));
        btnRefresh.setText("🔄");
        btnRefresh.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOOLBAR_REFRESH)));

        // Cluster
        // lblClusterList.setText(I18nUtil.get(I18nKeys.CLUSTER_LIST));  // Removed - no longer needed
    }

    /**
     * Initialize cluster tree with expandable structure
     */
    private void initializeClusterTree() {
        TreeItem<String> rootItem = new TreeItem<>(I18nUtil.get(I18nKeys.CLUSTER_LIST));
        rootItem.setExpanded(true);
        
        // Store type in userData
        TreeItemData rootData = new TreeItemData(TYPE_ROOT, null, null);
        treeItemDataMap.put(rootItem, rootData);

        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        for (ClusterConfig cluster : clusters) {
            addClusterToTree(rootItem, cluster);
        }

        clusterTreeView.setRoot(rootItem);
        clusterTreeView.setShowRoot(true);

        // Add context menu for cluster items
        setupContextMenu();

        // Handle tree item selection - single click selects, double click opens
        clusterTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleTreeItemSelected(newVal);
            }
        });
        
        // Handle double-click to open/expand
        clusterTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    TreeItemData data = treeItemDataMap.get(selectedItem);
                    if (data != null) {
                        if (TYPE_CLUSTER.equals(data.getType())) {
                            // Double-click on cluster - expand/collapse
                            selectedItem.setExpanded(!selectedItem.isExpanded());
                        } else if (!TYPE_ROOT.equals(data.getType())) {
                            // Double-click on function - open content
                            handleTreeItemActivated(selectedItem);
                        }
                    }
                }
            }
        });
    }

    private void addClusterToTree(TreeItem<String> rootItem, ClusterConfig cluster) {
        String displayName = cluster.getName();
        TreeItem<String> clusterItem = new TreeItem<>(displayName);
        TreeItemData clusterData = new TreeItemData(TYPE_CLUSTER, cluster.getId(), cluster);
        treeItemDataMap.put(clusterItem, clusterData);
        clusterItem.setExpanded(false);
        
        // Add sub-items for cluster functions
        TreeItem<String> overviewItem = new TreeItem<>(I18nUtil.get(I18nKeys.TAB_OVERVIEW));
        treeItemDataMap.put(overviewItem, new TreeItemData(TYPE_OVERVIEW, cluster.getId(), cluster));
        
        TreeItem<String> brokersItem = new TreeItem<>(I18nUtil.get(I18nKeys.TAB_BROKERS));
        treeItemDataMap.put(brokersItem, new TreeItemData(TYPE_BROKERS, cluster.getId(), cluster));
        
        TreeItem<String> topicsItem = new TreeItem<>(I18nUtil.get(I18nKeys.TAB_TOPICS));
        treeItemDataMap.put(topicsItem, new TreeItemData(TYPE_TOPICS, cluster.getId(), cluster));
        
        TreeItem<String> consumerGroupsItem = new TreeItem<>(I18nUtil.get(I18nKeys.TAB_CONSUMER_GROUPS));
        treeItemDataMap.put(consumerGroupsItem, new TreeItemData(TYPE_CONSUMER_GROUPS, cluster.getId(), cluster));
        
        TreeItem<String> aclItem = new TreeItem<>(I18nUtil.get(I18nKeys.TAB_ACL));
        treeItemDataMap.put(aclItem, new TreeItemData(TYPE_ACL, cluster.getId(), cluster));
        
        clusterItem.getChildren().addAll(overviewItem, brokersItem, topicsItem, consumerGroupsItem, aclItem);
        
        rootItem.getChildren().add(clusterItem);
        clusterTreeItems.put(cluster.getId(), clusterItem);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18nUtil.get(I18nKeys.COMMON_EDIT));
        editItem.setOnAction(e -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TreeItemData data = treeItemDataMap.get(selectedItem);
                if (data != null && TYPE_CLUSTER.equals(data.getType())) {
                    handleEditCluster(data.getClusterConfig());
                }
            }
        });
        
        MenuItem deleteItem = new MenuItem(I18nUtil.get(I18nKeys.COMMON_DELETE));
        deleteItem.setOnAction(e -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TreeItemData data = treeItemDataMap.get(selectedItem);
                if (data != null && TYPE_CLUSTER.equals(data.getType())) {
                    handleDeleteCluster(data.getClusterConfig());
                }
            }
        });
        
        contextMenu.getItems().addAll(editItem, deleteItem);

        clusterTreeView.setOnContextMenuRequested(event -> {
            TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TreeItemData data = treeItemDataMap.get(selectedItem);
                if (data != null && TYPE_CLUSTER.equals(data.getType())) {
                    contextMenu.show(clusterTreeView, event.getScreenX(), event.getScreenY());
                }
            }
        });
    }

    private void handleTreeItemSelected(TreeItem<String> item) {
        // Just selection, don't open content yet (wait for double-click)
        TreeItemData data = treeItemDataMap.get(item);
        if (data != null && TYPE_CLUSTER.equals(data.getType())) {
            updateStatus(I18nUtil.get(I18nKeys.CLUSTER_LIST) + ": " + item.getValue());
        }
    }

    private void handleTreeItemActivated(TreeItem<String> item) {
        TreeItemData data = treeItemDataMap.get(item);
        if (data == null) {
            return;
        }

        ClusterConfig cluster = data.getClusterConfig();
        String clusterId = data.getClusterId();
        
        // Ensure cluster is connected
        if (!clusterContentManagers.containsKey(clusterId)) {
            connectToCluster(cluster);
        }
        
        // Show appropriate content based on type
        ClusterContentManager manager = clusterContentManagers.get(clusterId);
        if (manager != null) {
            Node content = null;
            switch (data.getType()) {
                case TYPE_OVERVIEW:
                    content = manager.getOverviewContent();
                    break;
                case TYPE_BROKERS:
                    content = manager.getBrokersContent();
                    break;
                case TYPE_TOPICS:
                    content = manager.getTopicsContent();
                    break;
                case TYPE_CONSUMER_GROUPS:
                    content = manager.getConsumerGroupsContent();
                    break;
                case TYPE_ACL:
                    content = manager.getAclContent();
                    break;
            }
            
            if (content != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
            }
        }
    }

    private void connectToCluster(ClusterConfig cluster) {
        updateStatus(I18nUtil.get(I18nKeys.CLUSTER_CONNECTING, cluster.getName()));

        new Thread(() -> {
            boolean connected = ClusterService.getInstance().connect(cluster);
            
            Platform.runLater(() -> {
                if (connected) {
                    updateStatus(I18nUtil.get(I18nKeys.CLUSTER_CONNECTED, cluster.getName()));
                    
                    // Create content manager for this cluster
                    ClusterContentManager manager = new ClusterContentManager(cluster, this);
                    clusterContentManagers.put(cluster.getId(), manager);
                    manager.loadInitialData();
                } else {
                    updateStatus(I18nUtil.get(I18nKeys.CLUSTER_FAILED, cluster.getName()));
                    showError(I18nUtil.get(I18nKeys.DIALOG_ERROR_TITLE), 
                            I18nUtil.get(I18nKeys.CLUSTER_FAILED, cluster.getName()));
                }
            });
        }).start();
    }

    @FXML
    private void handleAddCluster() {
        // Create add cluster dialog
        Dialog<ClusterConfig> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.CLUSTER_ADD_TITLE));
        dialog.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_ADD_HEADER));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
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
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
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
                try {
                    Integer.parseInt(portField.getText().trim());
                    
                    // Check for duplicate cluster name or server
                    String newName = nameField.getText().trim();
                    String newServer = hostnameField.getText().trim() + ":" + portField.getText().trim();
                    List<ClusterConfig> existingClusters = ConfigManager.getInstance().getClusters();
                    
                    for (ClusterConfig existing : existingClusters) {
                        if (existing.getName().equalsIgnoreCase(newName)) {
                            showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), 
                                "Cluster name '" + newName + "' already exists!");
                            event.consume();
                            return;
                        }
                        if (existing.getBootstrapServers().equalsIgnoreCase(newServer)) {
                            showError(I18nUtil.get(I18nKeys.PRODUCER_ERROR_TITLE), 
                                "Server '" + newServer + "' already exists!");
                            event.consume();
                            return;
                        }
                    }
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
            
            // Add to tree
            TreeItem<String> rootItem = clusterTreeView.getRoot();
            addClusterToTree(rootItem, config);
            
            showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_ADD_SUCCESS, config.getName()));
        });
    }

    private void handleEditCluster(ClusterConfig cluster) {
        Dialog<ClusterConfig> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.CLUSTER_EDIT_TITLE));
        dialog.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_EDIT_HEADER));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(cluster.getName());
        TextField hostnameField = new TextField();
        TextField portField = new TextField();
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL");
        
        String bootstrapServers = cluster.getBootstrapServers();
        if (bootstrapServers != null && bootstrapServers.contains(":")) {
            String[] parts = bootstrapServers.split(":");
            hostnameField.setText(parts[0]);
            if (parts.length > 1) {
                portField.setText(parts[1].split(",")[0]);
            }
        } else {
            hostnameField.setText(bootstrapServers != null ? bootstrapServers : "localhost");
            portField.setText("9092");
        }
        
        String protocol = cluster.getSecurityProtocol();
        protocolCombo.setValue(protocol != null && !protocol.isEmpty() ? protocol : "PLAINTEXT");
        
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
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
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
            
            // Update tree item
            TreeItem<String> clusterTreeItem = clusterTreeItems.get(cluster.getId());
            if (clusterTreeItem != null) {
                clusterTreeItem.setValue(updatedCluster.getName());
            }
            
            showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_EDIT_SUCCESS, updatedCluster.getName()));
        });
    }

    private void handleDeleteCluster(ClusterConfig cluster) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtil.get(I18nKeys.CLUSTER_DELETE_TITLE));
        alert.setHeaderText(I18nUtil.get(I18nKeys.CLUSTER_DELETE_CONFIRM, cluster.getName()));
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ConfigManager.getInstance().deleteCluster(cluster.getId());
            
            // Remove from tree
            TreeItem<String> clusterTreeItem = clusterTreeItems.get(cluster.getId());
            if (clusterTreeItem != null && clusterTreeItem.getParent() != null) {
                clusterTreeItem.getParent().getChildren().remove(clusterTreeItem);
                clusterTreeItems.remove(cluster.getId());
            }
            
            // Remove content manager
            clusterContentManagers.remove(cluster.getId());
            
            // Clear content area if this cluster was displayed
            contentArea.getChildren().clear();
            
            showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.CLUSTER_DELETE_SUCCESS, cluster.getName()));
        }
    }

    @FXML
    private void handleRefreshTopics() {
        TreeItem<String> selectedItem = clusterTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            TreeItemData data = treeItemDataMap.get(selectedItem);
            if (data != null && data.getClusterId() != null) {
                ClusterContentManager manager = clusterContentManagers.get(data.getClusterId());
                if (manager != null) {
                    manager.refresh();
                }
            }
        }
    }

    @FXML
    private void handleSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.get(I18nKeys.SETTINGS_TITLE));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

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
            
            ConfigManager.getInstance().getConfig().getPreferences().setLanguage(lang);
            ConfigManager.getInstance().saveConfig();
            
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

    void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Helper class to store tree item data
    static class TreeItemData {
        private final String type;
        private final String clusterId;
        private final ClusterConfig clusterConfig;

        public TreeItemData(String type, String clusterId, ClusterConfig clusterConfig) {
            this.type = type;
            this.clusterId = clusterId;
            this.clusterConfig = clusterConfig;
        }

        public String getType() { return type; }
        public String getClusterId() { return clusterId; }
        public ClusterConfig getClusterConfig() { return clusterConfig; }
    }

    // Inner class to manage content for each cluster
    static class ClusterContentManager {
        private final ClusterConfig cluster;
        private final MainController mainController;
        
        // Content nodes (cached)
        private Node overviewContent;
        private Node brokersContent;
        private Node topicsContent;
        private Node consumerGroupsContent;
        private Node aclContent;
        
        // Data components for topics
        private TableView<TopicInfo> topicsTableView;
        private TextArea topicDetailsTextArea;
        private final ObservableList<TopicInfo> topicList = FXCollections.observableArrayList();
        
        // Data components for consumer groups
        private TableView<ConsumerGroupRow> consumerGroupTableView;
        private TableView<MemberRow> consumerGroupMembersTableView;
        private TableView<LagRow> consumerGroupLagTableView;
        private final ObservableList<ConsumerGroupRow> consumerGroupList = FXCollections.observableArrayList();
        private final ObservableList<MemberRow> memberList = FXCollections.observableArrayList();
        private final ObservableList<LagRow> lagList = FXCollections.observableArrayList();
        
        // Data for overview
        private Label overviewClusterName;
        private Label overviewBootstrapServers;
        private Label overviewBrokerCount;
        private Label overviewTopicCount;
        
        // Data for brokers
        private TableView<BrokerRow> brokersTableView;
        private final ObservableList<BrokerRow> brokerList = FXCollections.observableArrayList();

        public ClusterContentManager(ClusterConfig cluster, MainController mainController) {
            this.cluster = cluster;
            this.mainController = mainController;
        }

        public Node getOverviewContent() {
            if (overviewContent == null) {
                overviewContent = createOverviewContent();
            }
            return overviewContent;
        }

        public Node getBrokersContent() {
            if (brokersContent == null) {
                brokersContent = createBrokersContent();
            }
            return brokersContent;
        }

        public Node getTopicsContent() {
            if (topicsContent == null) {
                topicsContent = createTopicsContent();
            }
            return topicsContent;
        }

        public Node getConsumerGroupsContent() {
            if (consumerGroupsContent == null) {
                consumerGroupsContent = createConsumerGroupsContent();
            }
            return consumerGroupsContent;
        }

        public Node getAclContent() {
            if (aclContent == null) {
                aclContent = createAclContent();
            }
            return aclContent;
        }

        private Node createOverviewContent() {
            VBox vbox = new VBox(15);
            vbox.setPadding(new Insets(20));
            
            Label title = new Label(I18nUtil.get(I18nKeys.OVERVIEW_TITLE));
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            
            overviewClusterName = new Label(cluster.getName());
            overviewBootstrapServers = new Label(cluster.getBootstrapServers());
            overviewBrokerCount = new Label("Loading...");
            overviewTopicCount = new Label("Loading...");
            
            grid.add(new Label("Cluster Name:"), 0, 0);
            grid.add(overviewClusterName, 1, 0);
            grid.add(new Label("Bootstrap Servers:"), 0, 1);
            grid.add(overviewBootstrapServers, 1, 1);
            grid.add(new Label(I18nUtil.get(I18nKeys.OVERVIEW_BROKER_COUNT) + ":"), 0, 2);
            grid.add(overviewBrokerCount, 1, 2);
            grid.add(new Label(I18nUtil.get(I18nKeys.OVERVIEW_TOPIC_COUNT) + ":"), 0, 3);
            grid.add(overviewTopicCount, 1, 3);
            
            vbox.getChildren().addAll(title, grid);
            return vbox;
        }

        private Node createBrokersContent() {
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(16));
            
            Label title = new Label(I18nUtil.get(I18nKeys.BROKER_LIST));
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            brokersTableView = new TableView<>();
            brokersTableView.setItems(brokerList);
            
            TableColumn<BrokerRow, Integer> idCol = new TableColumn<>(I18nUtil.get(I18nKeys.BROKER_ID));
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setPrefWidth(100);
            
            TableColumn<BrokerRow, String> hostCol = new TableColumn<>(I18nUtil.get(I18nKeys.BROKER_HOST));
            hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
            hostCol.setPrefWidth(250);
            
            TableColumn<BrokerRow, Integer> portCol = new TableColumn<>(I18nUtil.get(I18nKeys.BROKER_PORT));
            portCol.setCellValueFactory(new PropertyValueFactory<>("port"));
            portCol.setPrefWidth(100);
            
            TableColumn<BrokerRow, String> rackCol = new TableColumn<>(I18nUtil.get(I18nKeys.BROKER_RACK));
            rackCol.setCellValueFactory(new PropertyValueFactory<>("rack"));
            rackCol.setPrefWidth(150);
            
            brokersTableView.getColumns().addAll(idCol, hostCol, portCol, rackCol);
            VBox.setVgrow(brokersTableView, javafx.scene.layout.Priority.ALWAYS);
            
            vbox.getChildren().addAll(title, brokersTableView);
            return vbox;
        }

        private Node createTopicsContent() {
            SplitPane splitPane = new SplitPane();
            splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
            splitPane.setDividerPositions(0.6);
            
            // Top: Topic list
            VBox topVBox = new VBox(10);
            topVBox.setPadding(new Insets(16));
            
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label lblTopicList = new Label(I18nUtil.get(I18nKeys.TOPIC_LIST));
            lblTopicList.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Button btnCreateTopic = new Button("➕");
            btnCreateTopic.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOPIC_CREATE)));
            btnCreateTopic.setOnAction(e -> handleCreateTopic());
            
            Button btnDeleteTopic = new Button("🗑");
            btnDeleteTopic.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOPIC_DELETE)));
            btnDeleteTopic.setOnAction(e -> handleDeleteTopic());
            
            headerBox.getChildren().addAll(lblTopicList, spacer, btnCreateTopic, btnDeleteTopic);
            
            topicsTableView = new TableView<>();
            topicsTableView.setItems(topicList);
            
            TableColumn<TopicInfo, String> nameCol = new TableColumn<>(I18nUtil.get(I18nKeys.TOPIC_NAME));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameCol.setPrefWidth(250);
            
            TableColumn<TopicInfo, Integer> partitionsCol = new TableColumn<>(I18nUtil.get(I18nKeys.TOPIC_PARTITIONS));
            partitionsCol.setCellValueFactory(new PropertyValueFactory<>("partitions"));
            partitionsCol.setPrefWidth(100);
            
            TableColumn<TopicInfo, Integer> replicationCol = new TableColumn<>(I18nUtil.get(I18nKeys.TOPIC_REPLICATION));
            replicationCol.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));
            replicationCol.setPrefWidth(120);
            
            topicsTableView.getColumns().addAll(nameCol, partitionsCol, replicationCol);
            topicsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showTopicDetails(newVal);
                }
            });
            
            VBox.setVgrow(topicsTableView, javafx.scene.layout.Priority.ALWAYS);
            topVBox.getChildren().addAll(headerBox, topicsTableView);
            
            // Bottom: Topic details
            VBox bottomVBox = new VBox(10);
            bottomVBox.setPadding(new Insets(16));
            
            Label lblTopicDetails = new Label(I18nUtil.get(I18nKeys.TOPIC_DETAILS));
            lblTopicDetails.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            topicDetailsTextArea = new TextArea();
            topicDetailsTextArea.setEditable(false);
            topicDetailsTextArea.setStyle("-fx-background-color: #F8F9FA;");
            VBox.setVgrow(topicDetailsTextArea, javafx.scene.layout.Priority.ALWAYS);
            
            bottomVBox.getChildren().addAll(lblTopicDetails, topicDetailsTextArea);
            
            splitPane.getItems().addAll(topVBox, bottomVBox);
            return splitPane;
        }

        private Node createConsumerGroupsContent() {
            SplitPane splitPane = new SplitPane();
            splitPane.setDividerPositions(0.4);
            
            // Left: Consumer group list
            VBox leftVBox = new VBox(10);
            leftVBox.setPadding(new Insets(10));
            
            Label lblGroupList = new Label(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LIST));
            lblGroupList.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            consumerGroupTableView = new TableView<>();
            consumerGroupTableView.setItems(consumerGroupList);
            
            TableColumn<ConsumerGroupRow, String> groupIdCol = new TableColumn<>(I18nUtil.get(I18nKeys.CONSUMER_GROUP_ID));
            groupIdCol.setCellValueFactory(new PropertyValueFactory<>("groupId"));
            groupIdCol.setPrefWidth(200);
            
            TableColumn<ConsumerGroupRow, String> stateCol = new TableColumn<>(I18nUtil.get(I18nKeys.CONSUMER_GROUP_STATE));
            stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
            stateCol.setPrefWidth(100);
            
            consumerGroupTableView.getColumns().addAll(groupIdCol, stateCol);
            consumerGroupTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showConsumerGroupDetails(newVal.getGroupId());
                }
            });
            
            VBox.setVgrow(consumerGroupTableView, javafx.scene.layout.Priority.ALWAYS);
            leftVBox.getChildren().addAll(lblGroupList, consumerGroupTableView);
            
            // Right: Consumer group details
            VBox rightVBox = new VBox(10);
            rightVBox.setPadding(new Insets(10));
            
            Label lblGroupDetails = new Label(I18nUtil.get(I18nKeys.CONSUMER_GROUP_DETAILS));
            lblGroupDetails.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            Label lblMembers = new Label("Members:");
            lblMembers.setStyle("-fx-font-weight: bold;");
            
            consumerGroupMembersTableView = new TableView<>();
            consumerGroupMembersTableView.setItems(memberList);
            
            TableColumn<MemberRow, String> memberIdCol = new TableColumn<>(I18nUtil.get(I18nKeys.CONSUMER_GROUP_MEMBER_ID));
            memberIdCol.setCellValueFactory(new PropertyValueFactory<>("memberId"));
            memberIdCol.setPrefWidth(200);
            
            consumerGroupMembersTableView.getColumns().add(memberIdCol);
            VBox.setVgrow(consumerGroupMembersTableView, javafx.scene.layout.Priority.ALWAYS);
            
            Label lblLag = new Label("Lag:");
            lblLag.setStyle("-fx-font-weight: bold;");
            
            consumerGroupLagTableView = new TableView<>();
            consumerGroupLagTableView.setItems(lagList);
            
            TableColumn<LagRow, String> topicCol = new TableColumn<>(I18nUtil.get(I18nKeys.TOPIC_NAME));
            topicCol.setCellValueFactory(new PropertyValueFactory<>("topic"));
            topicCol.setPrefWidth(150);
            
            TableColumn<LagRow, Integer> partitionCol = new TableColumn<>(I18nUtil.get(I18nKeys.QUERY_PARTITION));
            partitionCol.setCellValueFactory(new PropertyValueFactory<>("partition"));
            partitionCol.setPrefWidth(100);
            
            TableColumn<LagRow, Long> lagCol = new TableColumn<>(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LAG));
            lagCol.setCellValueFactory(new PropertyValueFactory<>("lag"));
            lagCol.setPrefWidth(100);
            
            consumerGroupLagTableView.getColumns().addAll(topicCol, partitionCol, lagCol);
            VBox.setVgrow(consumerGroupLagTableView, javafx.scene.layout.Priority.ALWAYS);
            
            rightVBox.getChildren().addAll(lblGroupDetails, lblMembers, consumerGroupMembersTableView, lblLag, consumerGroupLagTableView);
            
            splitPane.getItems().addAll(leftVBox, rightVBox);
            return splitPane;
        }

        private Node createAclContent() {
            VBox vbox = new VBox(20);
            vbox.setPadding(new Insets(20));
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label aclPlaceholder = new Label("ACL Management - Coming Soon");
            aclPlaceholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
            
            vbox.getChildren().add(aclPlaceholder);
            return vbox;
        }

        public void loadInitialData() {
            loadOverviewData();
            loadBrokers();
            loadTopics();
            loadConsumerGroups();
        }

        private void loadOverviewData() {
            new Thread(() -> {
                List<String> topicNames = TopicService.getInstance().listTopics(cluster.getId());
                
                Platform.runLater(() -> {
                    overviewTopicCount.setText(String.valueOf(topicNames.size()));
                    overviewBrokerCount.setText("N/A");
                });
            }).start();
        }

        private void loadBrokers() {
            new Thread(() -> {
                Platform.runLater(() -> {
                    brokerList.clear();
                    brokerList.add(new BrokerRow(0, "localhost", 9092, "rack1"));
                });
            }).start();
        }

        private void loadTopics() {
            new Thread(() -> {
                List<String> topicNames = TopicService.getInstance().listTopics(cluster.getId());
                List<TopicInfo> topics = new ArrayList<>();
                
                for (String topicName : topicNames) {
                    TopicInfo info = TopicService.getInstance().getTopicInfo(cluster.getId(), topicName);
                    if (info != null) {
                        topics.add(info);
                    }
                }
                
                Platform.runLater(() -> {
                    topicList.clear();
                    topicList.addAll(topics);
                    mainController.updateStatus(I18nUtil.get(I18nKeys.TOPIC_LOADED, topics.size()));
                });
            }).start();
        }

        private void showTopicDetails(TopicInfo topic) {
            new Thread(() -> {
                TopicInfo fullInfo = TopicService.getInstance().getTopicInfo(cluster.getId(), topic.getName());
                
                Platform.runLater(() -> {
                    if (fullInfo != null) {
                        StringBuilder details = new StringBuilder();
                        details.append(I18nUtil.get(I18nKeys.TOPIC_NAME)).append(": ").append(fullInfo.getName()).append("\n");
                        details.append(I18nUtil.get(I18nKeys.TOPIC_PARTITIONS)).append(": ").append(fullInfo.getPartitions()).append("\n");
                        details.append(I18nUtil.get(I18nKeys.TOPIC_REPLICATION)).append(": ").append(fullInfo.getReplicationFactor()).append("\n\n");
                        details.append("Configuration:\n");
                        fullInfo.getConfig().forEach((key, value) -> 
                            details.append("  ").append(key).append(": ").append(value).append("\n")
                        );
                        topicDetailsTextArea.setText(details.toString());
                    }
                });
            }).start();
        }

        private void loadConsumerGroups() {
            new Thread(() -> {
                List<String> groupIds = ConsumerGroupService.getInstance().listConsumerGroups(cluster.getId());
                
                Platform.runLater(() -> {
                    consumerGroupList.clear();
                    for (String groupId : groupIds) {
                        consumerGroupList.add(new ConsumerGroupRow(groupId, "", "", 0));
                    }
                    mainController.updateStatus(I18nUtil.get(I18nKeys.CONSUMER_GROUP_LOADED, groupIds.size()));
                    
                    loadConsumerGroupDetails();
                });
            }).start();
        }

        private void loadConsumerGroupDetails() {
            new Thread(() -> {
                for (ConsumerGroupRow row : consumerGroupList) {
                    ConsumerGroupInfo info = ConsumerGroupService.getInstance()
                            .getConsumerGroupInfo(cluster.getId(), row.getGroupId());
                    
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
            new Thread(() -> {
                ConsumerGroupInfo info = ConsumerGroupService.getInstance()
                        .getConsumerGroupInfo(cluster.getId(), groupId);
                
                Platform.runLater(() -> {
                    memberList.clear();
                    lagList.clear();
                    
                    if (info != null) {
                        for (ConsumerGroupInfo.MemberInfo member : info.getMembers()) {
                            String assignments = member.getAssignments().stream()
                                    .map(ConsumerGroupInfo.TopicPartition::toString)
                                    .reduce((a, b) -> a + ", " + b)
                                    .orElse("");
                            memberList.add(new MemberRow(member.getMemberId(), member.getClientId(), member.getHost(), assignments));
                        }
                        
                        info.getLag().forEach((tp, lag) -> {
                            Long offset = info.getOffsets().containsKey(tp) ? 
                                    info.getOffsets().get(tp).getOffset() : 0L;
                            lagList.add(new LagRow(tp.getTopic(), tp.getPartition(), offset, lag));
                        });
                    }
                });
            }).start();
        }

        private void handleCreateTopic() {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(I18nUtil.get(I18nKeys.TOPIC_CREATE_TITLE));
            dialog.setHeaderText(I18nUtil.get(I18nKeys.TOPIC_CREATE_HEADER));
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            
            TextField nameField = new TextField();
            TextField partitionsField = new TextField("1");
            TextField replicationField = new TextField("1");
            
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
                try {
                    int partitions = Integer.parseInt(partitionsField.getText().trim());
                    short replication = Short.parseShort(replicationField.getText().trim());
                    
                    boolean success = TopicService.getInstance().createTopic(cluster.getId(), topicName, partitions, replication);
                    
                    if (success) {
                        mainController.showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.TOPIC_CREATE_SUCCESS));
                        loadTopics();
                    } else {
                        mainController.showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_CREATE_ERROR));
                    }
                } catch (NumberFormatException e) {
                    mainController.showError(I18nUtil.get(I18nKeys.COMMON_ERROR), "Invalid number format");
                }
            }
        }

        private void handleDeleteTopic() {
            TopicInfo selectedTopic = topicsTableView.getSelectionModel().getSelectedItem();
            if (selectedTopic == null) {
                mainController.showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_DELETE_NO_SELECTION));
                return;
            }
            
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle(I18nUtil.get(I18nKeys.TOPIC_DELETE_TITLE));
            confirmAlert.setContentText(I18nUtil.get(I18nKeys.TOPIC_DELETE_CONFIRM, selectedTopic.getName()));
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = TopicService.getInstance().deleteTopic(cluster.getId(), selectedTopic.getName());
                
                if (success) {
                    mainController.showInfo(I18nUtil.get(I18nKeys.COMMON_SUCCESS), I18nUtil.get(I18nKeys.TOPIC_DELETE_SUCCESS));
                    loadTopics();
                } else {
                    mainController.showError(I18nUtil.get(I18nKeys.COMMON_ERROR), I18nUtil.get(I18nKeys.TOPIC_DELETE_ERROR));
                }
            }
        }

        public void refresh() {
            loadOverviewData();
            loadBrokers();
            loadTopics();
            loadConsumerGroups();
        }
    }

    // Data classes
    public static class BrokerRow {
        private final int id;
        private final String host;
        private final int port;
        private final String rack;

        public BrokerRow(int id, String host, int port, String rack) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.rack = rack;
        }

        public int getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getRack() { return rack; }
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
