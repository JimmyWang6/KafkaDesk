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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
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
    
    // Cluster tree (left side)
    @FXML private TreeView<String> clusterTreeView;

    // Content area (right side)
    @FXML private StackPane contentArea;

    // Status bar
    @FXML private Label statusLabel;

    private Stage stage;
    private final Map<String, ClusterContentManager> clusterContentManagers = new HashMap<>();
    private final Map<String, TreeItem<String>> clusterTreeItems = new HashMap<>();
    private final Map<TreeItem<String>, TreeItemData> treeItemDataMap = new HashMap<>();
    private TreeItem<String> rootTreeItem; // Store root for filtering
    
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
    }

    /**
     * Initialize cluster tree with expandable structure
     */
    private void initializeClusterTree() {
        TreeItem<String> rootItem = new TreeItem<>(I18nUtil.get(I18nKeys.CLUSTER_LIST));
        rootItem.setExpanded(true);
        rootTreeItem = rootItem; // Store for filtering
        
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
        dialog.initOwner(stage);
        
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
        
        centerDialogOnStage(dialog);
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
        dialog.initOwner(stage);
        
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
        
        centerDialogOnStage(dialog);
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
        alert.initOwner(stage);
        centerDialogOnStage(alert);
        
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
        dialog.initOwner(stage);

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

        centerDialogOnStage(dialog);
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
        alert.initOwner(stage);
        centerDialogOnStage(alert);
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
        alert.initOwner(stage);
        centerDialogOnStage(alert);
        alert.showAndWait();
    }

    void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.initOwner(stage);
        centerDialogOnStage(alert);
        alert.showAndWait();
    }
    
    /**
     * Center a dialog on the main application window
     */
    private void centerDialogOnStage(Dialog<?> dialog) {
        if (stage != null) {
            dialog.setOnShown(e -> {
                dialog.setX(stage.getX() + (stage.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(stage.getY() + (stage.getHeight() - dialog.getHeight()) / 2);
            });
        }
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
                // Load data immediately when overview is first accessed
                loadOverviewData();
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
            VBox mainContainer = new VBox(0);
            mainContainer.setStyle("-fx-background-color: #ffffff;");
            
            // Header with cluster name and connection status
            HBox header = createContentHeader("Cluster Overview");
            
            // Content area
            VBox vbox = new VBox(20);
            vbox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 30;");
            
            // Initialize labels if not already done
            if (overviewBrokerCount == null) {
                overviewBrokerCount = new Label("3");  // Mock data
            }
            if (overviewTopicCount == null) {
                overviewTopicCount = new Label("47");  // Mock data
            }
            
            // Metrics cards - 5 cards in total as per mockup
            GridPane metricsGrid = new GridPane();
            metricsGrid.setHgap(20);
            metricsGrid.setVgap(20);
            
            // Create metric cards
            VBox brokersCard = createMetricCard("🖥️", "Total Brokers", overviewBrokerCount);
            VBox topicsCard = createMetricCard("📄", "Total Topics", overviewTopicCount);
            VBox partitionsCard = createMetricCard("🔀", "Total Partitions", new Label("184"));  // Mock data
            VBox messagesCard = createMetricCard("⚡", "Messages/sec", new Label("12.4K"));  // Mock data
            VBox consumerGroupsCard = createMetricCard("👥", "Consumer Groups", new Label("23"));  // Mock data
            
            // First row: 3 cards
            metricsGrid.add(brokersCard, 0, 0);
            metricsGrid.add(topicsCard, 1, 0);
            metricsGrid.add(partitionsCard, 2, 0);
            
            // Second row: 2 cards
            metricsGrid.add(messagesCard, 0, 1);
            metricsGrid.add(consumerGroupsCard, 1, 1);
            
            // Make metric cards expand
            for (int i = 0; i < 3; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(33.33);
                col.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                metricsGrid.getColumnConstraints().add(col);
            }
            
            // Add brokers table below metrics
            VBox tableContainer = new VBox(0);
            tableContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                  "-fx-border-color: #e1e8ed; -fx-border-width: 1; -fx-border-radius: 12; " +
                                  "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
            
            // Table header
            HBox tableHeader = new HBox();
            tableHeader.setStyle("-fx-padding: 20 24 20 24; -fx-border-color: #e1e8ed; -fx-border-width: 0 0 1 0;");
            Label tableTitle = new Label("🖥️ Active Brokers");
            tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1a202c;");
            tableHeader.getChildren().add(tableTitle);
            
            // Initialize brokers table if not already done
            if (brokersTableView == null) {
                brokersTableView = new TableView<>();
                brokersTableView.setItems(brokerList);
            }
            
            brokersTableView.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");
            brokersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            // Clear existing columns and recreate
            brokersTableView.getColumns().clear();
            
            // ID Column with styled broker ID badge
            TableColumn<BrokerRow, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setCellFactory(col -> new TableCell<BrokerRow, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label(String.valueOf(item));
                        badge.getStyleClass().add("broker-id");
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
            idCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.08));
            
            TableColumn<BrokerRow, String> hostCol = new TableColumn<>("Host");
            hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
            hostCol.setCellFactory(col -> new TableCell<BrokerRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            });
            hostCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.25));
            
            TableColumn<BrokerRow, Integer> portCol = new TableColumn<>("Port");
            portCol.setCellValueFactory(new PropertyValueFactory<>("port"));
            portCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.08));
            
            // Status Column with badge
            TableColumn<BrokerRow, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setCellFactory(col -> new TableCell<BrokerRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label badge = new Label("● ONLINE");
                        badge.setStyle("-fx-background-color: linear-gradient(to right, #d1fae5 0%, #a7f3d0 100%); " +
                                     "-fx-text-fill: #065f46; -fx-padding: 5 12 5 12; -fx-background-radius: 20; " +
                                     "-fx-font-size: 11px; -fx-font-weight: 700; -fx-border-color: #6ee7b7; " +
                                     "-fx-border-width: 1; -fx-border-radius: 20;");
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
            statusCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.12));
            
            // Controller Column
            TableColumn<BrokerRow, Boolean> controllerCol = new TableColumn<>("Controller");
            controllerCol.setCellValueFactory(new PropertyValueFactory<>("controller"));
            controllerCol.setCellFactory(col -> new TableCell<BrokerRow, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item ? "✓ Yes" : "✗ No");
                    }
                }
            });
            controllerCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.10));
            
            // Partitions Column
            TableColumn<BrokerRow, Integer> partitionsCol = new TableColumn<>("Partitions");
            partitionsCol.setCellValueFactory(new PropertyValueFactory<>("partitions"));
            partitionsCol.setCellFactory(col -> new TableCell<BrokerRow, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(String.valueOf(item));
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            });
            partitionsCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.10));
            
            TableColumn<BrokerRow, Integer> leadersCol = new TableColumn<>("Leaders");
            leadersCol.setCellValueFactory(new PropertyValueFactory<>("leaders"));
            leadersCol.setCellFactory(col -> new TableCell<BrokerRow, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle(null);
                    } else {
                        setText(String.valueOf(item));
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            });
            leadersCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.10));
            
            TableColumn<BrokerRow, String> diskUsageCol = new TableColumn<>("Disk Usage");
            diskUsageCol.setCellValueFactory(new PropertyValueFactory<>("diskUsage"));
            diskUsageCol.setCellFactory(col -> new TableCell<BrokerRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        VBox container = new VBox(4);
                        Label text = new Label("47.3 GB / 100 GB (47%)");  // Mock data
                        text.setStyle("-fx-font-size: 12px;");
                        
                        // Progress bar
                        HBox progressBar = new HBox();
                        progressBar.setStyle("-fx-background-color: #e1e8ed; -fx-background-radius: 10; -fx-pref-height: 6;");
                        
                        Region fill = new Region();
                        fill.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); " +
                                    "-fx-background-radius: 10;");
                        fill.prefWidthProperty().bind(progressBar.widthProperty().multiply(0.47));
                        fill.setMaxHeight(6);
                        
                        progressBar.getChildren().add(fill);
                        container.getChildren().addAll(text, progressBar);
                        
                        setGraphic(container);
                        setText(null);
                    }
                }
            });
            diskUsageCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.15));
            
            // Actions Column
            TableColumn<BrokerRow, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setCellFactory(col -> new TableCell<BrokerRow, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox actions = new HBox(8);
                        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        
                        Label infoIcon = new Label("ℹ️");
                        infoIcon.getStyleClass().add("action-icon");
                        infoIcon.setTooltip(new Tooltip("View Details"));
                        
                        Label logsIcon = new Label("📋");
                        logsIcon.getStyleClass().add("action-icon");
                        logsIcon.setTooltip(new Tooltip("View Logs"));
                        
                        actions.getChildren().addAll(infoIcon, logsIcon);
                        setGraphic(actions);
                    }
                }
            });
            actionsCol.prefWidthProperty().bind(brokersTableView.widthProperty().multiply(0.12));
            
            brokersTableView.getColumns().addAll(idCol, hostCol, portCol, statusCol, controllerCol, 
                                                  partitionsCol, leadersCol, diskUsageCol, actionsCol);
            VBox.setVgrow(brokersTableView, javafx.scene.layout.Priority.ALWAYS);
            
            tableContainer.getChildren().addAll(tableHeader, brokersTableView);
            
            vbox.getChildren().addAll(metricsGrid, tableContainer);
            VBox.setVgrow(vbox, javafx.scene.layout.Priority.ALWAYS);
            
            mainContainer.getChildren().addAll(header, vbox);
            return mainContainer;
        }
        
        private HBox createContentHeader(String title) {
            HBox header = new HBox(12);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            header.setStyle("-fx-background-color: #ffffff; -fx-padding: 20 30 20 30; " +
                          "-fx-border-color: #f0f3f7; -fx-border-width: 0 0 2 0;");
            
            // Title
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #1a202c;");
            
            // Connection status badge
            HBox statusContainer = new HBox(6);
            statusContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            statusContainer.setStyle("-fx-background-color: linear-gradient(to right, #10b981 0%, #059669 100%); " +
                                   "-fx-padding: 6 14 6 14; -fx-background-radius: 20;");
            
            // Status dot
            Region statusDot = new Region();
            statusDot.setStyle("-fx-background-color: white; -fx-min-width: 8px; -fx-min-height: 8px; " +
                             "-fx-max-width: 8px; -fx-max-height: 8px; -fx-background-radius: 50%;");
            
            Label statusText = new Label("Connected");
            statusText.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600;");
            
            statusContainer.getChildren().addAll(statusDot, statusText);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            // Cluster name label
            Label clusterNameLabel = new Label(cluster.getName());
            clusterNameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #5a6c7d; -fx-font-weight: 500;");
            
            header.getChildren().addAll(titleLabel, statusContainer, spacer, clusterNameLabel);
            return header;
        }
        
        private VBox createMetricCard(String icon, String label, Label valueLabel) {
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 12; " +
                         "-fx-border-color: #e1e8ed; -fx-border-width: 1; -fx-border-radius: 12; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
            card.setMinHeight(120);
            
            // Icon
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 24px;");
            
            // Label
            Label metricLabel = new Label(label);
            metricLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096; -fx-font-weight: 600; " +
                                "-fx-text-transform: uppercase; -fx-letter-spacing: 0.5;");
            
            // Value
            valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; " +
                              "-fx-text-fill: #667eea;");
            
            card.getChildren().addAll(iconLabel, metricLabel, valueLabel);
            
            // Hover effect
            card.setOnMouseEntered(e -> {
                card.setStyle("-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 12; " +
                             "-fx-border-color: #667eea; -fx-border-width: 1; -fx-border-radius: 12; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 4); " +
                             "-fx-translate-y: -4;");
            });
            card.setOnMouseExited(e -> {
                card.setStyle("-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 12; " +
                             "-fx-border-color: #e1e8ed; -fx-border-width: 1; -fx-border-radius: 12; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
            });
            
            return card;
        }

        private Node createBrokersContent() {
            // Brokers are now shown in overview, redirect to overview
            return createOverviewContent();
        }

        private Node createTopicsContent() {
            VBox mainContainer = new VBox(0);
            mainContainer.setStyle("-fx-background-color: #ffffff;");
            
            // Header with cluster name and connection status
            HBox header = createContentHeader("Topics");
            
            // Content area
            VBox vbox = new VBox(0);
            vbox.setStyle("-fx-background-color: #f8fafc;");
            
            // Container for table with header
            VBox tableContainer = new VBox(0);
            tableContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                  "-fx-border-color: #e1e8ed; -fx-border-width: 1; -fx-border-radius: 12; " +
                                  "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");
            VBox.setMargin(tableContainer, new Insets(30, 30, 30, 30));
            
            // Table header with action buttons
            HBox tableHeader = new HBox(8);
            tableHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tableHeader.setStyle("-fx-padding: 20 24 20 24; -fx-border-color: #e1e8ed; -fx-border-width: 0 0 1 0;");
            
            Label tableTitle = new Label("📊 Topics");
            tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1a202c;");
            HBox.setHgrow(tableTitle, javafx.scene.layout.Priority.ALWAYS);
            
            Button btnCreateTopic = new Button("➕");
            btnCreateTopic.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOPIC_CREATE)));
            btnCreateTopic.getStyleClass().add("toolbar-button");
            btnCreateTopic.setOnAction(e -> handleCreateTopic());
            
            Button btnDeleteTopic = new Button("🗑");
            btnDeleteTopic.setTooltip(new Tooltip(I18nUtil.get(I18nKeys.TOPIC_DELETE)));
            btnDeleteTopic.getStyleClass().add("toolbar-button");
            btnDeleteTopic.setOnAction(e -> handleDeleteTopic());
            
            tableHeader.getChildren().addAll(tableTitle, btnCreateTopic, btnDeleteTopic);
            
            topicsTableView = new TableView<>();
            topicsTableView.setItems(topicList);
            topicsTableView.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 12 12;");
            
            TableColumn<TopicInfo, String> nameCol = new TableColumn<>("Topic Name");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameCol.prefWidthProperty().bind(topicsTableView.widthProperty().multiply(0.35));
            
            TableColumn<TopicInfo, Integer> partitionsCol = new TableColumn<>("Partitions");
            partitionsCol.setCellValueFactory(new PropertyValueFactory<>("partitions"));
            partitionsCol.prefWidthProperty().bind(topicsTableView.widthProperty().multiply(0.15));
            
            TableColumn<TopicInfo, Integer> replicationCol = new TableColumn<>("Replication");
            replicationCol.setCellValueFactory(new PropertyValueFactory<>("replicationFactor"));
            replicationCol.prefWidthProperty().bind(topicsTableView.widthProperty().multiply(0.15));
            
            TableColumn<TopicInfo, String> retentionCol = new TableColumn<>("Retention Time (hours)");
            retentionCol.setCellValueFactory(new PropertyValueFactory<>("retentionTime"));
            retentionCol.prefWidthProperty().bind(topicsTableView.widthProperty().multiply(0.35));
            
            topicsTableView.getColumns().addAll(nameCol, partitionsCol, replicationCol, retentionCol);
            
            // Double-click to show topic details
            topicsTableView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    TopicInfo selectedTopic = topicsTableView.getSelectionModel().getSelectedItem();
                    if (selectedTopic != null) {
                        showTopicDetailView(selectedTopic);
                    }
                }
            });
            
            VBox.setVgrow(topicsTableView, javafx.scene.layout.Priority.ALWAYS);
            
            tableContainer.getChildren().addAll(tableHeader, topicsTableView);
            vbox.getChildren().add(tableContainer);
            VBox.setVgrow(tableContainer, javafx.scene.layout.Priority.ALWAYS);
            VBox.setVgrow(vbox, javafx.scene.layout.Priority.ALWAYS);
            
            mainContainer.getChildren().addAll(header, vbox);
            return mainContainer;
        }
        
        /**
         * Show detailed view for a topic with partitions and consumers tabs
         */
        private void showTopicDetailView(TopicInfo topic) {
            // Create a new window/dialog for topic details
            Stage detailStage = new Stage();
            detailStage.setTitle("Topic Details: " + topic.getName());
            detailStage.initOwner(mainController.stage);
            
            TabPane detailTabPane = new TabPane();
            detailTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            detailTabPane.setStyle("-fx-background-color: white;");
            
            // Tab 1: Partitions
            Tab partitionsTab = new Tab("Partitions");
            partitionsTab.setContent(createPartitionsView(topic));
            
            // Tab 2: Consumers
            Tab consumersTab = new Tab("Consumers");
            consumersTab.setContent(createTopicConsumersView(topic));
            
            detailTabPane.getTabs().addAll(partitionsTab, consumersTab);
            
            Scene scene = new Scene(detailTabPane, 800, 600);
            detailStage.setScene(scene);
            
            // Center the stage on the owner window
            if (mainController.stage != null) {
                detailStage.setOnShown(e -> {
                    detailStage.setX(mainController.stage.getX() + (mainController.stage.getWidth() - detailStage.getWidth()) / 2);
                    detailStage.setY(mainController.stage.getY() + (mainController.stage.getHeight() - detailStage.getHeight()) / 2);
                });
            }
            
            detailStage.show();
        }
        
        /**
         * Create partitions view showing partition details
         */
        private Node createPartitionsView(TopicInfo topic) {
            VBox vbox = new VBox(0);
            vbox.setStyle("-fx-background-color: white;");
            
            TableView<PartitionRow> partitionsTable = new TableView<>();
            partitionsTable.setStyle("-fx-background-color: white;");
            
            ObservableList<PartitionRow> partitionData = FXCollections.observableArrayList();
            
            TableColumn<PartitionRow, Integer> partitionCol = new TableColumn<>("Partition");
            partitionCol.setCellValueFactory(new PropertyValueFactory<>("partition"));
            partitionCol.prefWidthProperty().bind(partitionsTable.widthProperty().multiply(0.20));
            
            TableColumn<PartitionRow, Long> minOffsetCol = new TableColumn<>("Min Offset");
            minOffsetCol.setCellValueFactory(new PropertyValueFactory<>("minOffset"));
            minOffsetCol.prefWidthProperty().bind(partitionsTable.widthProperty().multiply(0.25));
            
            TableColumn<PartitionRow, Long> maxOffsetCol = new TableColumn<>("Max Offset");
            maxOffsetCol.setCellValueFactory(new PropertyValueFactory<>("maxOffset"));
            maxOffsetCol.prefWidthProperty().bind(partitionsTable.widthProperty().multiply(0.25));
            
            TableColumn<PartitionRow, String> leaderCol = new TableColumn<>("Leader");
            leaderCol.setCellValueFactory(new PropertyValueFactory<>("leader"));
            leaderCol.prefWidthProperty().bind(partitionsTable.widthProperty().multiply(0.30));
            
            partitionsTable.getColumns().addAll(partitionCol, minOffsetCol, maxOffsetCol, leaderCol);
            partitionsTable.setItems(partitionData);
            
            // Load partition data in background
            new Thread(() -> {
                // Simulate loading partition data - in real implementation, fetch from Kafka
                for (int i = 0; i < topic.getPartitions(); i++) {
                    int partition = i;
                    PartitionRow row = new PartitionRow(partition, 0L, 1000L + (partition * 100), "Broker " + (partition % 3));
                    Platform.runLater(() -> partitionData.add(row));
                }
            }).start();
            
            VBox.setVgrow(partitionsTable, javafx.scene.layout.Priority.ALWAYS);
            vbox.getChildren().add(partitionsTable);
            
            return vbox;
        }
        
        /**
         * Create consumers view for a specific topic
         */
        private Node createTopicConsumersView(TopicInfo topic) {
            VBox vbox = new VBox(0);
            vbox.setStyle("-fx-background-color: white;");
            
            TableView<TopicConsumerRow> consumersTable = new TableView<>();
            consumersTable.setStyle("-fx-background-color: white;");
            
            ObservableList<TopicConsumerRow> consumerData = FXCollections.observableArrayList();
            
            TableColumn<TopicConsumerRow, String> groupNameCol = new TableColumn<>("Consumer Group");
            groupNameCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
            groupNameCol.prefWidthProperty().bind(consumersTable.widthProperty().multiply(0.30));
            
            TableColumn<TopicConsumerRow, String> stateCol = new TableColumn<>("State");
            stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
            stateCol.prefWidthProperty().bind(consumersTable.widthProperty().multiply(0.20));
            
            TableColumn<TopicConsumerRow, String> coordinatorCol = new TableColumn<>("Coordinator ID");
            coordinatorCol.setCellValueFactory(new PropertyValueFactory<>("coordinatorId"));
            coordinatorCol.prefWidthProperty().bind(consumersTable.widthProperty().multiply(0.25));
            
            TableColumn<TopicConsumerRow, Long> lagCol = new TableColumn<>("Message Backlog");
            lagCol.setCellValueFactory(new PropertyValueFactory<>("messageLag"));
            lagCol.prefWidthProperty().bind(consumersTable.widthProperty().multiply(0.25));
            
            consumersTable.getColumns().addAll(groupNameCol, stateCol, coordinatorCol, lagCol);
            consumersTable.setItems(consumerData);
            
            // Load consumer data for this topic in background
            new Thread(() -> {
                List<String> groupIds = ConsumerGroupService.getInstance().listConsumerGroups(cluster.getId());
                
                for (String groupId : groupIds) {
                    ConsumerGroupInfo info = ConsumerGroupService.getInstance()
                            .getConsumerGroupInfo(cluster.getId(), groupId);
                    
                    if (info != null) {
                        // Check if this group consumes from the selected topic
                        long totalLag = 0;
                        boolean consumesFromTopic = false;
                        
                        for (Map.Entry<ConsumerGroupInfo.TopicPartition, Long> entry : info.getLag().entrySet()) {
                            if (entry.getKey().getTopic().equals(topic.getName())) {
                                consumesFromTopic = true;
                                totalLag += entry.getValue();
                            }
                        }
                        
                        if (consumesFromTopic) {
                            long finalTotalLag = totalLag;
                            TopicConsumerRow row = new TopicConsumerRow(
                                groupId,
                                info.getState(),
                                String.valueOf(info.getCoordinatorId()),
                                finalTotalLag
                            );
                            Platform.runLater(() -> consumerData.add(row));
                        }
                    }
                }
            }).start();
            
            VBox.setVgrow(consumersTable, javafx.scene.layout.Priority.ALWAYS);
            vbox.getChildren().add(consumersTable);
            
            return vbox;
        }

        private Node createConsumerGroupsContent() {
            VBox vbox = new VBox(0);
            vbox.setStyle("-fx-background-color: white;");
            
            consumerGroupTableView = new TableView<>();
            consumerGroupTableView.setItems(consumerGroupList);
            consumerGroupTableView.setStyle("-fx-background-color: white;");
            
            TableColumn<ConsumerGroupRow, String> groupIdCol = new TableColumn<>("Group ID");
            groupIdCol.setCellValueFactory(new PropertyValueFactory<>("groupId"));
            groupIdCol.prefWidthProperty().bind(consumerGroupTableView.widthProperty().multiply(0.40));
            
            TableColumn<ConsumerGroupRow, String> stateCol = new TableColumn<>("State");
            stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
            stateCol.prefWidthProperty().bind(consumerGroupTableView.widthProperty().multiply(0.20));
            
            TableColumn<ConsumerGroupRow, String> coordinatorCol = new TableColumn<>("Coordinator");
            coordinatorCol.setCellValueFactory(new PropertyValueFactory<>("coordinator"));
            coordinatorCol.prefWidthProperty().bind(consumerGroupTableView.widthProperty().multiply(0.25));
            
            TableColumn<ConsumerGroupRow, Integer> membersCol = new TableColumn<>("Members");
            membersCol.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
            membersCol.prefWidthProperty().bind(consumerGroupTableView.widthProperty().multiply(0.15));
            
            consumerGroupTableView.getColumns().addAll(groupIdCol, stateCol, coordinatorCol, membersCol);
            VBox.setVgrow(consumerGroupTableView, javafx.scene.layout.Priority.ALWAYS);
            
            vbox.getChildren().add(consumerGroupTableView);
            return vbox;
        }

        private Node createAclContent() {
            VBox vbox = new VBox(0);
            vbox.setStyle("-fx-background-color: white; -fx-padding: 20;");
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label aclPlaceholder = new Label("ACL Management - Coming Soon");
            aclPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            
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
                    if (overviewTopicCount != null) {
                        overviewTopicCount.setText(String.valueOf(topicNames.size()));
                    }
                    if (overviewBrokerCount != null) {
                        overviewBrokerCount.setText("N/A");
                    }
                });
            }).start();
        }

        private void loadBrokers() {
            new Thread(() -> {
                Platform.runLater(() -> {
                    brokerList.clear();
                    // Sample data - in real implementation, this would fetch actual broker data
                    brokerList.add(new BrokerRow(0, "localhost", 9092, "rack1", "45%", 12, 24));
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
            confirmAlert.initOwner(mainController.stage);
            mainController.centerDialogOnStage(confirmAlert);
            
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
        private final String diskUsage;
        private final int leaders;
        private final int replicas;

        public BrokerRow(int id, String host, int port, String rack, String diskUsage, int leaders, int replicas) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.rack = rack;
            this.diskUsage = diskUsage;
            this.leaders = leaders;
            this.replicas = replicas;
        }

        public int getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getRack() { return rack; }
        public String getDiskUsage() { return diskUsage; }
        public int getLeaders() { return leaders; }
        public int getReplicas() { return replicas; }
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
    
    public static class PartitionRow {
        private final int partition;
        private final Long minOffset;
        private final Long maxOffset;
        private final String leader;

        public PartitionRow(int partition, Long minOffset, Long maxOffset, String leader) {
            this.partition = partition;
            this.minOffset = minOffset;
            this.maxOffset = maxOffset;
            this.leader = leader;
        }

        public int getPartition() { return partition; }
        public Long getMinOffset() { return minOffset; }
        public Long getMaxOffset() { return maxOffset; }
        public String getLeader() { return leader; }
    }
    
    public static class TopicConsumerRow {
        private final String groupName;
        private final String state;
        private final String coordinatorId;
        private final Long messageLag;

        public TopicConsumerRow(String groupName, String state, String coordinatorId, Long messageLag) {
            this.groupName = groupName;
            this.state = state;
            this.coordinatorId = coordinatorId;
            this.messageLag = messageLag;
        }

        public String getGroupName() { return groupName; }
        public String getState() { return state; }
        public String getCoordinatorId() { return coordinatorId; }
        public Long getMessageLag() { return messageLag; }
    }
}
