package com.kafkadesk.core.config;

import com.kafkadesk.model.ClusterConfig;
import com.kafkadesk.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration manager
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.kafkadesk";
    private static final String CONFIG_FILE = "config.json";
    
    private static ConfigManager instance;
    private AppConfig config;

    private ConfigManager() {
        loadConfig();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Load configuration
     */
    private void loadConfig() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            File configFile = new File(CONFIG_DIR, CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config = JsonUtil.fromJson(fis, AppConfig.class);
                    if (config == null) {
                        config = new AppConfig();
                    }
                    logger.info("Configuration loaded successfully");
                }
            } else {
                config = new AppConfig();
                logger.info("No existing configuration found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            config = new AppConfig();
        }
    }

    /**
     * Save configuration
     */
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_DIR, CONFIG_FILE);
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                JsonUtil.writeJson(fos, config);
                logger.info("Configuration saved successfully");
            }
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    /**
     * Get all cluster configurations
     */
    public List<ClusterConfig> getClusters() {
        return config.getClusters();
    }

    /**
     * Add cluster configuration
     */
    public void addCluster(ClusterConfig cluster) {
        config.getClusters().add(cluster);
        saveConfig();
    }

    /**
     * Update cluster configuration
     */
    public void updateCluster(ClusterConfig cluster) {
        List<ClusterConfig> clusters = config.getClusters();
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).getId().equals(cluster.getId())) {
                clusters.set(i, cluster);
                saveConfig();
                return;
            }
        }
    }

    /**
     * Delete cluster configuration
     */
    public void deleteCluster(String clusterId) {
        config.getClusters().removeIf(c -> c.getId().equals(clusterId));
        saveConfig();
    }

    /**
     * Get cluster configuration by ID
     */
    public ClusterConfig getClusterById(String clusterId) {
        return config.getClusters().stream()
                .filter(c -> c.getId().equals(clusterId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get application configuration
     */
    public AppConfig getConfig() {
        return config;
    }

    /**
     * Application configuration class
     */
    public static class AppConfig {
        private String version = "1.0";
        private List<ClusterConfig> clusters = new ArrayList<>();
        private Preferences preferences = new Preferences();
        private WindowConfig window = new WindowConfig();

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<ClusterConfig> getClusters() {
            return clusters;
        }

        public void setClusters(List<ClusterConfig> clusters) {
            this.clusters = clusters;
        }

        public Preferences getPreferences() {
            return preferences;
        }

        public void setPreferences(Preferences preferences) {
            this.preferences = preferences;
        }

        public WindowConfig getWindow() {
            return window;
        }

        public void setWindow(WindowConfig window) {
            this.window = window;
        }
    }

    /**
     * User preferences
     */
    public static class Preferences {
        private String theme = "light";
        private String language = "zh_CN";
        private boolean autoConnect = true;
        private String lastSelectedCluster;

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }

        public String getLastSelectedCluster() {
            return lastSelectedCluster;
        }

        public void setLastSelectedCluster(String lastSelectedCluster) {
            this.lastSelectedCluster = lastSelectedCluster;
        }
    }

    /**
     * Window configuration
     */
    public static class WindowConfig {
        private double width = 1200;
        private double height = 800;
        private boolean maximized = false;

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }

        public boolean isMaximized() {
            return maximized;
        }

        public void setMaximized(boolean maximized) {
            this.maximized = maximized;
        }
    }
}
