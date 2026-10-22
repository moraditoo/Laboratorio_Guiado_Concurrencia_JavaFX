package ni.edu.uam.modelo;

public enum EstadoSistema {
    EN_ESPERA("En espera"),
    EJECUTANDO("Ejecutando"),
    DETENIENDO("Deteniendo"),
    DETENIDO("Detenido"),
    FINALIZADO("Finalizado");

    private final String etiqueta;

    EstadoSistema(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}