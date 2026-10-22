package ni.edu.uam.tareas;

import ni.edu.uam.modelo.ContadorCompartido;

public class ProcesoVentanilla implements Runnable {
    private final String nombre;
    private final ContadorCompartido contador;
    private final int operacionesTotales;
    private final VentanillaListener listener;
    private volatile boolean activo = true;
    private int procesadas = 0;

    public ProcesoVentanilla(String nombre, ContadorCompartido contador, int operacionesTotales, VentanillaListener listener) {
        this.nombre = nombre;
        this.contador = contador;
        this.operacionesTotales = operacionesTotales;
        this.listener = listener;
    }

    public void detener() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public int getProcesadas() {
        return procesadas;
    }

    public int getOperacionesTotales() {
        return operacionesTotales;
    }

    public boolean estaCompletado() {
        return procesadas >= operacionesTotales;
    }

    public void reiniciarProgreso() {
        this.procesadas = 0;
        this.activo = true;
    }

    @Override
    public void run() {
        while (activo && !Thread.currentThread().isInterrupted() && procesadas < operacionesTotales) {
            try {
                Thread.sleep(250);
                contador.incrementarSincronizado();
                procesadas++;
                if (listener != null) {
                    listener.alProcesarSolicitud(nombre, contador.getContadorSincronizado());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (listener != null && procesadas >= operacionesTotales) {
            listener.alFinalizar(nombre);
        }
    }
}