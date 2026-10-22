package ni.edu.uam.modelo;

public class PruebaCondicionCarrera {
    private static int contadorInseguro = 0;

    public static void main(String[] args) throws InterruptedException {
        for (int corrida = 1; corrida <= 5; corrida++) {
            contadorInseguro = 0;
            Thread h1 = new Thread(() -> {
                for (int i = 0; i < 1000; i++) contadorInseguro++;
            });
            Thread h2 = new Thread(() -> {
                for (int i = 0; i < 1000; i++) contadorInseguro++;
            });

            h1.start();
            h2.start();
            h1.join();
            h2.join();

            System.out.println("Corrida " + corrida + " (No segura): " + contadorInseguro + " / Esperado: 2000");
        }

        ContadorCompartido seguro = new ContadorCompartido();
        Thread h3 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) seguro.incrementarSincronizado();
        });
        Thread h4 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) seguro.incrementarSincronizado();
        });

        h3.start();
        h4.start();
        h3.join();
        h4.join();

        System.out.println("Corrida Sincronizada: " + seguro.getContadorSincronizado() + " / Esperado: 2000");
    }
}