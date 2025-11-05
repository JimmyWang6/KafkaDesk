package com.kafkadesk.ui;

import com.kafkadesk.core.config.ConfigManager;
import com.kafkadesk.core.service.ClusterService;
import com.kafkadesk.core.service.ConsumerService;
import com.kafkadesk.core.service.ProducerService;
import com.kafkadesk.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KafkaDesk JavaFX main application
 */
public class KafkaDeskApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDeskApplication.class);
    private static final String APP_TITLE = "KafkaDesk - Kafka 桌面客户端";
    private static final String MAIN_FXML = "/fxml/main.fxml";

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting KafkaDesk application...");

            // Load main interface
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Scene scene = new Scene(loader.load());

            // Load CSS styles
            String css = getClass().getResource("/css/light-theme.css").toExternalForm();
            scene.getStylesheets().add(css);

            // Set window properties
            ConfigManager.WindowConfig windowConfig = ConfigManager.getInstance()
                    .getConfig()
                    .getWindow();

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setWidth(windowConfig.getWidth());
            primaryStage.setHeight(windowConfig.getHeight());
            primaryStage.setMaximized(windowConfig.isMaximized());

            // Set window icon (if available)
            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/icons/app-icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.warn("Failed to load application icon", e);
            }

            // Get controller and set stage
            MainController controller = loader.getController();
            controller.setStage(primaryStage);

            // Window close event
            primaryStage.setOnCloseRequest(event -> {
                onApplicationClose(primaryStage);
            });

            primaryStage.show();
            logger.info("KafkaDesk application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Cleanup work when application closes
     */
    private void onApplicationClose(Stage stage) {
        logger.info("Closing KafkaDesk application...");

        try {
            // Save window configuration
            ConfigManager.WindowConfig windowConfig = ConfigManager.getInstance()
                    .getConfig()
                    .getWindow();
            windowConfig.setWidth(stage.getWidth());
            windowConfig.setHeight(stage.getHeight());
            windowConfig.setMaximized(stage.isMaximized());
            ConfigManager.getInstance().saveConfig();

            // Close all connections
            ClusterService.getInstance().closeAllConnections();
            ProducerService.getInstance().closeAllProducers();
            ConsumerService.getInstance().closeAllConsumers();

            logger.info("KafkaDesk application closed successfully");
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Application stop called");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
