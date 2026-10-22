package ni.edu.uam.modelo;

import java.util.concurrent.atomic.AtomicInteger;

public class ContadorCompartido {
    private int contadorSincronizado = 0;
    private final AtomicInteger contadorAtomico = new AtomicInteger(0);

    public synchronized void incrementarSincronizado() {
        contadorSincronizado++;
    }

    public synchronized int getContadorSincronizado() {
        return contadorSincronizado;
    }

    public void incrementarAtomico() {
        contadorAtomico.incrementAndGet();
    }

    public int getContadorAtomico() {
        return contadorAtomico.get();
    }

    public synchronized void reiniciar() {
        contadorSincronizado = 0;
        contadorAtomico.set(0);
    }
}