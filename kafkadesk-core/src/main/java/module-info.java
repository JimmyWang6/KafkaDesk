module com.kafkadesk.core {
    requires com.kafkadesk.model;
    requires com.kafkadesk.utils;
    requires kafka.clients;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    exports com.kafkadesk.core.config;
    exports com.kafkadesk.core.service;
}
