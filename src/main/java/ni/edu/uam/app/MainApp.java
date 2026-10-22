package ni.edu.uam.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        VentanaPrincipal root = new VentanaPrincipal();
        Scene scene = new Scene(root, 580, 480);
        primaryStage.setTitle("Sistema de Ventanillas Concurrentes - UAM");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}