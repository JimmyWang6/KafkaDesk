package com.kafkadesk.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kafkadesk.model.KafkaConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for persisting Kafka connections to disk
 */
public class ConnectionPersistence {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPersistence.class);
    private static final String APP_DIR = ".kafkadesk";
    private static final String CONNECTIONS_FILE = "connections.json";
    
    private final Gson gson;
    private final Path connectionsFilePath;

    public ConnectionPersistence() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, APP_DIR);
        
        try {
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create application directory", e);
        }
        
        this.connectionsFilePath = appDir.resolve(CONNECTIONS_FILE);
    }

    /**
     * Save connections to disk
     */
    public void saveConnections(List<KafkaConnection> connections) {
        try (Writer writer = new FileWriter(connectionsFilePath.toFile())) {
            gson.toJson(connections, writer);
            logger.info("Saved {} connections to disk", connections.size());
        } catch (IOException e) {
            logger.error("Failed to save connections", e);
        }
    }

    /**
     * Load connections from disk
     */
    public List<KafkaConnection> loadConnections() {
        if (!Files.exists(connectionsFilePath)) {
            logger.info("No saved connections found");
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(connectionsFilePath.toFile())) {
            Type listType = new TypeToken<ArrayList<KafkaConnection>>(){}.getType();
            List<KafkaConnection> connections = gson.fromJson(reader, listType);
            logger.info("Loaded {} connections from disk", connections.size());
            return connections != null ? connections : new ArrayList<>();
        } catch (IOException e) {
            logger.error("Failed to load connections", e);
            return new ArrayList<>();
        }
    }

    /**
     * Add or update a connection
     */
    public void saveConnection(KafkaConnection connection, List<KafkaConnection> connections) {
        if (connection.getId() == null || connection.getId().isEmpty()) {
            connection.setId(UUID.randomUUID().toString());
        }
        
        connections.removeIf(c -> c.getId().equals(connection.getId()));
        connections.add(connection);
        saveConnections(connections);
    }

    /**
     * Delete a connection
     */
    public void deleteConnection(String connectionId, List<KafkaConnection> connections) {
        connections.removeIf(c -> c.getId().equals(connectionId));
        saveConnections(connections);
    }
}
