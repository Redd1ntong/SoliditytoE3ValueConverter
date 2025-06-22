module com.urjc.application {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.urjc.application to javafx.fxml;
    exports com.urjc.application;
    requires com.fasterxml.jackson.databind;
}
