package ni.edu.uam.tareas;

public interface VentanillaListener {
    void alProcesarSolicitud(String nombreVentanilla, int totalActual);
    void alCambiarEstado(String mensaje);
    void alFinalizar(String nombreVentanilla);
}