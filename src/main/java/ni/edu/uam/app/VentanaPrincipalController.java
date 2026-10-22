package ni.edu.uam.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
    @FXML private Button btnReiniciar;
    @FXML private ProgressBar progresoOperaciones;
    @FXML private Label lblEstado;
    @FXML private Label lblContador;
    @FXML private Label lblMonitor;
    @FXML private TextArea txtLog;

    private static final int OPERACIONES_POR_VENTANILLA = 20;
    private static final int TOTAL_ESPERADO = OPERACIONES_POR_VENTANILLA * 2;

    private final ContadorCompartido contadorCompartido = new ContadorCompartido();
    private ProcesoVentanilla tareaV1;
    private ProcesoVentanilla tareaV2;
    private Thread hiloV1;
    private Thread hiloV2;
    private Thread hiloMonitor;
    private MonitorHilos monitor;

    private int ventanillasTerminadas = 0;

    @FXML
    public void initialize() {
        tareaV1 = new ProcesoVentanilla("Ventanilla-A", contadorCompartido, OPERACIONES_POR_VENTANILLA, this);
        tareaV2 = new ProcesoVentanilla("Ventanilla-B", contadorCompartido, OPERACIONES_POR_VENTANILLA, this);
    }

    @FXML
    private void manejarIniciar() {
        if (tareaV1.estaCompletado() && tareaV2.estaCompletado()) {
            txtLog.appendText(">> Todas las solicitudes ya fueron completadas. Use Reiniciar.\n");
            return;
        }

        btnIniciar.setDisable(true);
        btnDetener.setDisable(false);
        btnReiniciar.setDisable(true);

        tareaV1.activar();
        tareaV2.activar();

        lblEstado.setText("Estado: " + EstadoSistema.EJECUTANDO.getEtiqueta());

        Map<String, Thread> mapaHilos = new HashMap<>();

        if (!tareaV1.estaCompletado()) {
            hiloV1 = new Thread(tareaV1, "Thread-Ventanilla-A");
            mapaHilos.put("Ventanilla-A", hiloV1);
        }
        if (!tareaV2.estaCompletado()) {
            hiloV2 = new Thread(tareaV2, "Thread-Ventanilla-B");
            mapaHilos.put("Ventanilla-B", hiloV2);
        }

        monitor = new MonitorHilos(mapaHilos, snapshot -> lblMonitor.setText("Hilos: " + snapshot));
        hiloMonitor = new Thread(monitor, "Thread-Monitor");
        hiloMonitor.setDaemon(true);

        if (hiloV1 != null && hiloV1.getState() == Thread.State.NEW) hiloV1.start();
        if (hiloV2 != null && hiloV2.getState() == Thread.State.NEW) hiloV2.start();
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
                if (hiloV1 != null && hiloV1.isAlive()) hiloV1.join();
                if (hiloV2 != null && hiloV2.isAlive()) hiloV2.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                lblEstado.setText("Estado: " + EstadoSistema.DETENIDO.getEtiqueta());
                btnIniciar.setText("Reanudar");
                btnIniciar.setDisable(false);
                btnReiniciar.setDisable(false);
                txtLog.appendText(">> Proceso pausado en transaccion " + contadorCompartido.getContadorSincronizado() + "/" + TOTAL_ESPERADO + ".\n");
            });
        }).start();
    }

    @FXML
    private void manejarReiniciar() {
        btnIniciar.setDisable(true);
        btnDetener.setDisable(true);
        btnReiniciar.setDisable(true);

        new Thread(() -> {
            if (tareaV1 != null) tareaV1.detener();
            if (tareaV2 != null) tareaV2.detener();
            if (hiloV1 != null) hiloV1.interrupt();
            if (hiloV2 != null) hiloV2.interrupt();

            try {
                if (hiloV1 != null && hiloV1.isAlive()) hiloV1.join();
                if (hiloV2 != null && hiloV2.isAlive()) hiloV2.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            contadorCompartido.reiniciar();
            tareaV1.reiniciarProgreso();
            tareaV2.reiniciarProgreso();

            Platform.runLater(() -> {
                ventanillasTerminadas = 0;
                progresoOperaciones.setProgress(0.0);
                lblEstado.setText("Estado: " + EstadoSistema.EN_ESPERA.getEtiqueta());
                lblContador.setText("Progreso: 0 / " + TOTAL_ESPERADO);
                lblMonitor.setText("Hilos: Sin actividad");
                btnIniciar.setText("Iniciar");
                btnIniciar.setDisable(false);
                txtLog.clear();
                txtLog.appendText(">> Sistema restablecido a cero.\n");
            });
        }).start();
    }

    @Override
    public void alProcesarSolicitud(String nombreVentanilla, int totalActual) {
        Platform.runLater(() -> {
            double ratio = (double) totalActual / TOTAL_ESPERADO;
            progresoOperaciones.setProgress(ratio);
            lblContador.setText("Progreso: " + totalActual + " / " + TOTAL_ESPERADO);
            txtLog.appendText(nombreVentanilla + " procesó. Acumulado: " + totalActual + "\n");
        });
    }

    @Override
    public void alCambiarEstado(String mensaje) {
        Platform.runLater(() -> txtLog.appendText(mensaje + "\n"));
    }

    @Override
    public void alFinalizar(String nombreVentanilla) {
        Platform.runLater(() -> {
            txtLog.appendText(">> " + nombreVentanilla + " completo todas sus tareas.\n");
            ventanillasTerminadas++;
            if (ventanillasTerminadas == 2) {
                progresoOperaciones.setProgress(1.0);
                lblEstado.setText("Estado: " + EstadoSistema.FINALIZADO.getEtiqueta());
                btnIniciar.setText("Iniciar");
                btnIniciar.setDisable(true);
                btnDetener.setDisable(true);
                btnReiniciar.setDisable(false);
            }
        });
    }
}