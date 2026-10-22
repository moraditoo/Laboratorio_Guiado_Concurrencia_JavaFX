# Sistema de Ventanillas Concurrentes en JavaFX

## Contexto y Planteamiento del Problema

En entornos de alta demanda operativa (entidades bancarias, oficinas de atención y cajas de facturación), múltiples terminales o ventanillas procesan transacciones de manera simultánea compartiendo una misma base de datos o acumulador central. 

Este proyecto modela un sistema de procesamiento transaccional con dos ventanillas concurrentes (`Ventanilla-A` y `Ventanilla-B`) que incrementan un contador global de solicitudes completadas. El reto arquitectónico radica en:
1. **Evitar pérdidas de cómputo:** Proteger el recurso compartido contra condiciones de carrera mediante primitivas de sincronización atómicas y exclusión mutua.
2. **Preservar la responsividad gráfica:** Desacoplar completamente el trabajo pesado del hilo de interfaz (`JavaFX Application Thread`), evitando bloqueos o congelamiento de la ventana.
3. **Cancelación cooperativa y reanudación segura:** Implementar un mecanismo de parada controlada sin recurrir a métodos destructivos como `Thread.stop()`, permitiendo reanudar o reiniciar el lote de operaciones sin dejar hilos huérfanos o inconsistencias en memoria.

## Distribución de Trabajo y Gestión en GitHub

El equipo está compuesto por 3 integrantes. La responsabilidad técnica y las ramas asignadas se dividieron equitativamente:

| Integrante | Rama Git | Rol y Estaciones Asignadas |
| :--- | :--- | :--- |
| **Brandon** | `feature/brandon` | **Estación 3:** Sincronización de Recursos Compartidos, Modelos de Exclusión Mutua (`synchronized`, `AtomicInteger`) y Pruebas de Condición de Carrera. |
| **Andres** | `feature/andres` | **Estaciones 1 y 4:** Tareas de Fondo con `Runnable`, Control de Ciclo de Vida, Detención Cooperativa (`volatile` + `interrupt`) y Reanudación de Lote. |
| **Dylan** | `feature/dylan` | **Estaciones 2 y 5:** Interfaz Gráfica FXML Minimalista, Monitor de Estados Asíncrono (`getState`), Despacho UI con `Platform.runLater()` y Gestión del Stage. |

### Flujo de Integración y Reglas de Repositorio

* `main`: Rama de producción bloqueada para commits directos. Solo se integra mediante Pull Requests aprobados y validados.
* Cada desarrollador trabaja en su rama individual, ejecuta pruebas locales y abre un PR con la evidencia correspondiente.
* El líder de integración resuelve los conflictos de merge preservando la consistencia modular antes de publicar el release final.

---

## Matriz de Pruebas y Evidencias

| Prueba | Resultado Esperado | Evidencia / Hallazgo Verificado |
| :--- | :--- | :--- |
| **Dos ventanillas** | Ambas trabajan y la ventana responde | `Ventanilla-A` y `Ventanilla-B` intercalan transacciones concurrentemente; la ventana se puede redimensionar y arrastrar fluidamente sin degradar los FPS. |
| **Conteo concurrente** | Total correcto sin pérdidas | El contador acumulador alcanza exactamente 40/40 solicitudes mediante sincronización intrínseca; en la prueba sin sincronizar (`PruebaCondicionCarrera`) se registran pérdidas de 100 a 300 conteos por condición de carrera. |
| **Cancelación** | Las tareas finalizan cooperativamente | Al pulsar «Detener», la bandera `volatile activo` pasa a `false` y se envía `interrupt()`; los hilos atrapan `InterruptedException`, rompen el ciclo y entran en estado `DETENIDO` sin errores en consola. |
| **Reinicio** | No quedan hilos de la sesión anterior | Se comprueba con `join()` que los subprocesos previos alcancen el estado `TERMINATED` fuera del hilo de UI antes de inicializar o reanudar un nuevo ciclo de trabajo. |
| **Estados** | Observaciones explicadas con evidencia | El monitor en segundo plano captura en tiempo real las transiciones del ciclo de vida: `NEW`, `RUNNABLE`, `TIMED_WAITING` y `TERMINATED`. |

---

## Análisis Técnico y Respuestas a la Rúbrica

### 1. ¿Qué error apareció antes de sincronizar?
Se manifestó una **condición de carrera** (*race condition*). A nivel de bytecode en la JVM, la instrucción `total++` no es atómica: se descompone en tres operaciones individuales (`getfield` para leer el valor, `iadd` para incrementarlo en el registro de la CPU y `putfield` para guardarlo de vuelta en la memoria principal). Cuando dos núcleos de procesamiento ejecutan estas etapas simultáneamente sin barreras de sincronización, uno de los hilos sobreescribe el valor del otro, perdiendo ciclos de incremento. La exclusión mutua mediante `synchronized` o `AtomicInteger` (que utiliza la instrucción nativa del procesador *Compare-And-Swap*) asegura que la operación se realice de forma indivisible.

### 2. ¿Cómo se evitó bloquear JavaFX?
El **JavaFX Application Thread** tiene la responsabilidad única de gestionar los eventos del sistema operativo (clics, teclado) y el refresco visual de la escena. Si se invoca `Thread.sleep()`, bucles intensivos o llamadas sincrónicas a `Thread.join()` dentro del hilo de la interfaz, el despachador se detiene y la ventana se congela. Se resolvió mediante tres principios:
1. El procesamiento de las ventanillas corre en subprocesos independientes (`Thread-Ventanilla-A` y `Thread-Ventanilla-B`).
2. Las operaciones bloqueantes de espera (`join()`) para detener o reiniciar se ejecutan dentro de un hilo trabajador en background.
3. La sincronización hacia la UI se canaliza exclusivamente a través de la cola asíncrona de `Platform.runLater()`.

### 3. ¿Qué condiciones se verificaron para detener y reiniciar?
Nunca se utilizó el método obsoleto y propenso a corrupciones `Thread.stop()`. Se utilizó una detención cooperativa en dos niveles:
* Una bandera `volatile boolean activo` que deshabilita la optimización por caché local de cada CPU y garantiza visibilidad inmediata entre hilos.
* Verificación continua de `Thread.currentThread().isInterrupted()` y captura controlada de `InterruptedException` para reactivar la interrupción y abandonar inmediatamente el bucle si el hilo estaba suspendido en `Thread.sleep()`.

Para el reinicio o la reanudación, se aseguró que los hilos anteriores estuvieran totalmente finalizados comprobando `isAlive()` y ejecutando `join()` previo a instanciar las nuevas ejecuciones.

### 4. ¿Qué mejoraría para procesar 100,000 solicitudes?
* **Pool de Hilos con `ExecutorService`:** En lugar de crear hilos manuales que saturan la memoria del sistema operativo y aumentan el costo de *context switching*, se emplearía un `ThreadPoolExecutor` con una cola de tareas acotada (`BlockingQueue`).
* **Actualización por Lotes (*Batching*):** Despachar 100,000 invocaciones individuales con `Platform.runLater()` colapsaría la cola de renderizado de JavaFX. Se debe desacoplar el procesamiento del renderizado, agrupando las actualizaciones visuales cada 100 operaciones o utilizando un `AnimationTimer` sincronizado a 60 FPS para refrescar los controles visuales únicamente cuando haya cambios acumulados.
