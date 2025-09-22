module com.organization.hr.pub_manager {
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;
    requires javafx.controls;
    requires javafx.fxml;

    opens com.organization.hr.pub_manager to javafx.fxml;
    exports com.organization.hr.pub_manager;
}
