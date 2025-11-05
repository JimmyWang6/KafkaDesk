module com.kafkadesk.ui {
    requires com.kafkadesk.core;
    requires com.kafkadesk.model;
    requires com.kafkadesk.utils;
    requires javafx.controls;
    requires javafx.fxml;
    requires kafka.clients;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    opens com.kafkadesk.ui to javafx.fxml;
    opens com.kafkadesk.ui.controller to javafx.fxml;
    
    exports com.kafkadesk.ui;
    exports com.kafkadesk.ui.controller;
    exports com.kafkadesk.ui.util;
}
