module ni.edu.uam.laboratorio_guiado_concurrencia_javafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.laboratorio_guiado_concurrencia_javafx to javafx.fxml;
    exports ni.edu.uam.laboratorio_guiado_concurrencia_javafx;
}