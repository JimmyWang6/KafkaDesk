module com.kafkadesk.model {
    requires com.fasterxml.jackson.databind;
    
    exports com.kafkadesk.model;
    opens com.kafkadesk.model to com.fasterxml.jackson.databind;
}
