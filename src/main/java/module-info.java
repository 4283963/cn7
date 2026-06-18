module com.textadventure.editor {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports com.textadventure.editor;
    exports com.textadventure.editor.model;
    exports com.textadventure.editor.ui;
    exports com.textadventure.editor.service;
    exports com.textadventure.editor.dao;
    exports com.textadventure.editor.db;

    opens com.textadventure.editor to javafx.fxml;
    opens com.textadventure.editor.ui to javafx.fxml;
}
