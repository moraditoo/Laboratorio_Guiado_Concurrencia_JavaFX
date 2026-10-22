package ni.edu.uam.tareas;

import javafx.application.Platform;
import java.util.Map;
import java.util.function.Consumer;

public class MonitorHilos implements Runnable {
    private final Map<String, Thread> hilosObservados;
    private final Consumer<String> consumidorEstado;
    private volatile boolean monitoreando = true;

    public MonitorHilos(Map<String, Thread> hilosObservados, Consumer<String> consumidorEstado) {
        this.hilosObservados = hilosObservados;
        this.consumidorEstado = consumidorEstado;
    }

    public void detener() {
        this.monitoreando = false;
    }

    @Override
    public void run() {
        while (monitoreando && !Thread.currentThread().isInterrupted()) {
            StringBuilder snapshot = new StringBuilder();
            boolean todosTerminados = true;

            for (Map.Entry<String, Thread> entry : hilosObservados.entrySet()) {
                Thread.State state = entry.getValue().getState();
                snapshot.append("[").append(entry.getKey()).append(": ").append(state).append("] ");
                if (state != Thread.State.TERMINATED) {
                    todosTerminados = false;
                }
            }

            Platform.runLater(() -> consumidorEstado.accept(snapshot.toString()));

            if (todosTerminados) {
                break;
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}