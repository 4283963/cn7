package com.textadventure.editor;

import com.textadventure.editor.db.DatabaseConnection;
import com.textadventure.editor.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseConnection.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            primaryStage.setTitle("文字冒险游戏剧情编辑器");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> {
                try {
                    DatabaseConnection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("启动失败", e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
