package ni.edu.uam.tareas;

import ni.edu.uam.modelo.ContadorCompartido;

public class ProcesoVentanilla implements Runnable {
    private final String nombre;
    private final ContadorCompartido contador;
    private final int operacionesTotales;
    private final VentanillaListener listener;
    private volatile boolean activo = true;

    public ProcesoVentanilla(String nombre, ContadorCompartido contador, int operacionesTotales, VentanillaListener listener) {
        this.nombre = nombre;
        this.contador = contador;
        this.operacionesTotales = operacionesTotales;
        this.listener = listener;
    }

    public void detener() {
        this.activo = false;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public void run() {
        int procesadas = 0;

        // Ciclo con verificaciones cooperativas para detención limpia
        while (activo && !Thread.currentThread().isInterrupted() && procesadas < operacionesTotales) {
            try {
                Thread.sleep(250); // Simula el tiempo de procesamiento de cada solicitud

                contador.incrementarSincronizado();
                procesadas++;

                if (listener != null) {
                    listener.alProcesarSolicitud(nombre, contador.getContadorSincronizado());
                }
            } catch (InterruptedException e) {
                // Restaura la bandera de interrupción y rompe el ciclo para salir de inmediato
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (listener != null) {
            listener.alFinalizar(nombre);
        }
    }
}