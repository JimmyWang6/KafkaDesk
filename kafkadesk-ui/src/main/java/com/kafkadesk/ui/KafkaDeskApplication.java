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
 * KafkaDesk JavaFX 主应用程序
 */
public class KafkaDeskApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDeskApplication.class);
    private static final String APP_TITLE = "KafkaDesk - Kafka 桌面客户端";
    private static final String MAIN_FXML = "/fxml/main.fxml";

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting KafkaDesk application...");

            // 加载主界面
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Scene scene = new Scene(loader.load());

            // 加载 CSS 样式
            String css = getClass().getResource("/css/light-theme.css").toExternalForm();
            scene.getStylesheets().add(css);

            // 设置窗口属性
            ConfigManager.WindowConfig windowConfig = ConfigManager.getInstance()
                    .getConfig()
                    .getWindow();

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setWidth(windowConfig.getWidth());
            primaryStage.setHeight(windowConfig.getHeight());
            primaryStage.setMaximized(windowConfig.isMaximized());

            // 设置窗口图标（如果有的话）
            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/icons/app-icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.warn("Failed to load application icon", e);
            }

            // 获取控制器并设置 stage
            MainController controller = loader.getController();
            controller.setStage(primaryStage);

            // 窗口关闭事件
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
     * 应用程序关闭时的清理工作
     */
    private void onApplicationClose(Stage stage) {
        logger.info("Closing KafkaDesk application...");

        try {
            // 保存窗口配置
            ConfigManager.WindowConfig windowConfig = ConfigManager.getInstance()
                    .getConfig()
                    .getWindow();
            windowConfig.setWidth(stage.getWidth());
            windowConfig.setHeight(stage.getHeight());
            windowConfig.setMaximized(stage.isMaximized());
            ConfigManager.getInstance().saveConfig();

            // 关闭所有连接
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
