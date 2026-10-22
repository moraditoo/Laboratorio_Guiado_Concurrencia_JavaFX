package ni.edu.uam.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import ni.edu.uam.modelo.ContadorCompartido;
import ni.edu.uam.modelo.EstadoSistema;
import ni.edu.uam.tareas.MonitorHilos;
import ni.edu.uam.tareas.ProcesoVentanilla;
import ni.edu.uam.tareas.VentanillaListener;

import java.util.HashMap;
import java.util.Map;

public class VentanaPrincipalController implements VentanillaListener {

    @FXML private Button btnIniciar;
    @FXML private Button btnDetener;
    @FXML private Label lblEstado;
    @FXML private Label lblContador;
    @FXML private Label lblMonitor;
    @FXML private TextArea txtLog;

    private final ContadorCompartido contadorCompartido = new ContadorCompartido();
    private ProcesoVentanilla tareaV1;
    private ProcesoVentanilla tareaV2;
    private Thread hiloV1;
    private Thread hiloV2;
    private Thread hiloMonitor;
    private MonitorHilos monitor;

    private int ventanillasTerminadas = 0;

    @FXML
    private void manejarIniciar() {
        btnIniciar.setDisable(true);
        btnDetener.setDisable(false);
        txtLog.clear();
        contadorCompartido.reiniciar();
        ventanillasTerminadas = 0;

        lblEstado.setText("Estado: " + EstadoSistema.EJECUTANDO.getEtiqueta());
        lblContador.setText("Contador Global: 0");

        tareaV1 = new ProcesoVentanilla("Ventanilla-A", contadorCompartido, 20, this);
        tareaV2 = new ProcesoVentanilla("Ventanilla-B", contadorCompartido, 20, this);

        hiloV1 = new Thread(tareaV1, "Thread-Ventanilla-A");
        hiloV2 = new Thread(tareaV2, "Thread-Ventanilla-B");

        Map<String, Thread> mapaHilos = new HashMap<>();
        mapaHilos.put("Ventanilla-A", hiloV1);
        mapaHilos.put("Ventanilla-B", hiloV2);

        monitor = new MonitorHilos(mapaHilos, snapshot -> lblMonitor.setText("Estados: " + snapshot));
        hiloMonitor = new Thread(monitor, "Thread-Monitor");
        hiloMonitor.setDaemon(true);

        hiloV1.start();
        hiloV2.start();
        hiloMonitor.start();
    }

    @FXML
    private void manejarDetener() {
        lblEstado.setText("Estado: " + EstadoSistema.DETENIENDO.getEtiqueta());
        btnDetener.setDisable(true);

        new Thread(() -> {
            if (tareaV1 != null) tareaV1.detener();
            if (tareaV2 != null) tareaV2.detener();
            if (hiloV1 != null) hiloV1.interrupt();
            if (hiloV2 != null) hiloV2.interrupt();

            try {
                if (hiloV1 != null) hiloV1.join();
                if (hiloV2 != null) hiloV2.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                lblEstado.setText("Estado: " + EstadoSistema.DETENIDO.getEtiqueta());
                btnIniciar.setDisable(false);
                txtLog.appendText(">> Operaciones detenidas ordenadamente.\n");
            });
        }).start();
    }

    @Override
    public void alProcesarSolicitud(String nombreVentanilla, int totalActual) {
        Platform.runLater(() -> {
            lblContador.setText("Contador Global: " + totalActual);
            txtLog.appendText(nombreVentanilla + " proceso transaccion. Total: " + totalActual + "\n");
        });
    }

    @Override
    public void alCambiarEstado(String mensaje) {
        Platform.runLater(() -> txtLog.appendText(mensaje + "\n"));
    }

    @Override
    public void alFinalizar(String nombreVentanilla) {
        Platform.runLater(() -> {
            txtLog.appendText(nombreVentanilla + " ha concluido.\n");
            ventanillasTerminadas++;
            if (ventanillasTerminadas == 2) {
                lblEstado.setText("Estado: " + EstadoSistema.FINALIZADO.getEtiqueta());
                btnIniciar.setDisable(false);
                btnDetener.setDisable(true);
            }
        });
    }
}