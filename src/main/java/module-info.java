module com.organization.hr.pub_manager {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.organization.hr.pub_manager to javafx.fxml;
    exports com.organization.hr.pub_manager;
}