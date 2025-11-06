package com.kafkadesk.ui.helper;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.model.ClusterConfig;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/**
 * Helper class for cluster-related operations
 * Extracted from MainController to reduce complexity
 */
public class ClusterOperationsHelper {
    
    /**
     * Parse cluster name from display name format "name (servers)"
     */
    public static String parseClusterNameFromDisplay(String displayName) {
        if (displayName == null || !displayName.contains("(")) {
            return displayName;
        }
        return displayName.substring(0, displayName.indexOf("(")).trim();
    }
    
    /**
     * Find cluster config by name
     */
    public static Optional<ClusterConfig> findClusterByName(String clusterName) {
        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        return clusters.stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst();
    }
    
    /**
     * Create cluster add/edit dialog
     */
    public static Dialog<ClusterConfig> createClusterDialog(
            ClusterConfig cluster,
            String title,
            String header,
            String nameLabel,
            String hostLabel,
            String portLabel,
            String protocolLabel) {
        
        Dialog<ClusterConfig> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(cluster != null ? cluster.getName() : "");
        TextField hostnameField = new TextField();
        TextField portField = new TextField();
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL");
        
        if (cluster != null) {
            // Parse bootstrap servers to get hostname and port
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
            protocolCombo.setValue((protocol != null && !protocol.isEmpty()) ? protocol : "PLAINTEXT");
        } else {
            hostnameField.setText("localhost");
            portField.setText("9092");
            protocolCombo.setValue("PLAINTEXT");
        }
        
        grid.add(new Label(nameLabel), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(hostLabel), 0, 1);
        grid.add(hostnameField, 1, 1);
        grid.add(new Label(portLabel), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label(protocolLabel), 0, 3);
        grid.add(protocolCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ClusterConfig config = cluster != null ? cluster : new ClusterConfig();
                if (cluster != null) {
                    config.setId(cluster.getId());
                    config.setCreatedAt(cluster.getCreatedAt());
                }
                config.setName(nameField.getText().trim());
                config.setBootstrapServers(hostnameField.getText().trim() + ":" + portField.getText().trim());
                config.setSecurityProtocol(protocolCombo.getValue());
                return config;
            }
            return null;
        });
        
        return dialog;
    }
    
    /**
     * Add input validation to cluster dialog
     */
    public static void addValidationToClusterDialog(
            Dialog<ClusterConfig> dialog,
            String nameEmptyError,
            String hostEmptyError,
            String portEmptyError,
            String portInvalidError,
            ErrorDisplayCallback errorCallback) {
        
        javafx.scene.control.Button okButton = 
            (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            GridPane grid = (GridPane) dialog.getDialogPane().getContent();
            TextField nameField = (TextField) grid.getChildren().get(1);
            TextField hostnameField = (TextField) grid.getChildren().get(3);
            TextField portField = (TextField) grid.getChildren().get(5);
            
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                errorCallback.showError(nameEmptyError);
                event.consume();
            } else if (hostnameField.getText() == null || hostnameField.getText().trim().isEmpty()) {
                errorCallback.showError(hostEmptyError);
                event.consume();
            } else if (portField.getText() == null || portField.getText().trim().isEmpty()) {
                errorCallback.showError(portEmptyError);
                event.consume();
            } else {
                try {
                    Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException e) {
                    errorCallback.showError(portInvalidError);
                    event.consume();
                }
            }
        });
    }
    
    /**
     * Callback interface for error display
     */
    public interface ErrorDisplayCallback {
        void showError(String message);
    }
    
    /**
     * Delete cluster by name
     */
    public static boolean deleteCluster(String clusterName) {
        List<ClusterConfig> clusters = ConfigManager.getInstance().getClusters();
        Optional<ClusterConfig> cluster = clusters.stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst();
        
        if (cluster.isPresent()) {
            ConfigManager.getInstance().deleteCluster(cluster.get().getId());
            return true;
        }
        return false;
    }
}
