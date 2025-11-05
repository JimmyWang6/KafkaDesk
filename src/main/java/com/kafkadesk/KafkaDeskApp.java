package com.kafkadesk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application for KafkaDesk
 */
public class KafkaDeskApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDeskApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the main FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            BorderPane root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            
            primaryStage.setTitle("KafkaDesk - Kafka Management Tool");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            logger.info("KafkaDesk application started");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        logger.info("KafkaDesk application stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
