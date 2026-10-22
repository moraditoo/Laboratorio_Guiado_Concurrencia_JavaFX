module ni.edu.uam {
    requires javafx.controls;
    requires javafx.fxml;

    opens ni.edu.uam.app to javafx.fxml;
    opens ni.edu.uam.modelo to javafx.base;

    exports ni.edu.uam.app;
    exports ni.edu.uam.modelo;
    exports ni.edu.uam.tareas;
}