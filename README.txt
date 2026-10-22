# Laboratorio Guiado: Concurrencia en Java y JavaFX

Aplicación de escritorio concurrente desarrollada para la asignatura de **Programación de aplicaciones de escritorio** de la **Universidad Americana (UAM)**.

---

## 1. Integrantes, Ramas y Distribución Técnica

| Integrante | Rama | Módulo / Estaciones Asignadas |
| :--- | :--- | :--- |
| **Brandon** | `feature/brandon` | **Estación 3:** Sincronización de Recursos Compartidos, Modelos de Exclusión Mutua (`synchronized` / `AtomicInteger`) y Pruebas de Condición de Carrera. |
| **Andres** | `feature/andres` | **Estaciones 1 y 4:** Ciclo de vida con `Runnable`, Procesos en Segundo Plano, Detención Cooperativa (`volatile` + `interrupt`) y Reanudación de Lote[cite: 1]. |
| **Dylan** | `feature/dylan` | **Estaciones 2 y 5:** Interfaz Gráfica FXML, Monitor de Estados (`getState`), Despacho UI con `Platform.runLater()` y Control de Escena[cite: 1]. |

---

## 2. Flujo de Trabajo y Ramas en GitHub

El repositorio sigue un modelo de integración estricta para garantizar trazabilidad[cite: 1]:
* `main`: Rama de producción protegida. Únicamente el líder integra cambios mediante Pull Requests aprobados[cite: 1].
* `feature/brandon`: Desarrollo del modelo y pruebas de atomicidad[cite: 1].
* `feature/andres`: Implementación de los hilos de trabajo y detención[cite: 1].
* `feature/dylan`: Ensamblaje visual, controlador FXML y monitor asíncrono[cite: 1].

---

## 3. Matriz de Pruebas y Evidencias

| Prueba | Resultado Esperado | Evidencia / Hallazgo |
| :--- | :--- | :--- |
| **Dos ventanillas**[cite: 1, 3] | Ambas trabajan y la ventana responde[cite: 1, 3] | `Ventanilla-A` y `Ventanilla-B` intercalan transacciones en el registro en tiempo real; la ventana se puede arrastrar y redimensionar a 60 FPS sin retraso[cite: 1]. |
| **Conteo concurrente**[cite: 1, 3] | Total correcto sin pérdidas[cite: 1, 3] | El `ContadorCompartido` alcanza exactamente 40/40 solicitudes mediante sincronización intrínseca; en la prueba sin sincronizar (`PruebaCondicionCarrera`) se evidencian pérdidas de entre 100 y 300 conteos por condición de carrera[cite: 1]. |
| **Cancelación**[cite: 1, 3] | Las tareas finalizan cooperativamente[cite: 1, 3] | Al pulsar «Detener», la bandera `volatile activo` pasa a `false` y se envía `interrupt()`; los hilos atrapan `InterruptedException`, rompen el bucle y cambian al estado `DETENIDO` sin excepciones no controladas[cite: 1]. |
| **Reinicio**[cite: 1, 3] | No quedan hilos de la sesión anterior[cite: 1, 3] | Se verifica con `join()` que los hilos previos alcancen el estado `TERMINATED` en segundo plano antes de permitir una nueva ejecución limpia o reanudada[cite: 1]. |
| **Estados**[cite: 1, 3] | Observaciones explicadas con evidencia[cite: 1, 3] | El hilo monitor registra en tiempo real transiciones: `NEW` (antes de `start()`), `TIMED_WAITING` (durante `Thread.sleep()`), `RUNNABLE` (procesando incremento) y `TERMINATED` (al culminar)[cite: 1]. |

---

## 4. Respuestas Técnicas y Justificación (Rúbrica)

### ¿Qué error apareció antes de sincronizar?
Apareció una **condición de carrera** (*race condition*)[cite: 1]. La operación `contador++` consta de tres instrucciones a nivel de bytecode: leer el valor de memoria (`getfield`), sumar uno (`iadd`) y almacenar el resultado (`putfield`). Cuando dos hilos ejecutan estas fases simultáneamente sobre el mismo núcleo de memoria sin exclusión mutua, uno de los hilos sobrescribe el valor del otro, provocando pérdida de incrementos[cite: 1]. Al encapsular la operación en un método `synchronized` o usar `AtomicInteger` (basado en hardware CAS - *Compare-And-Swap*), se garantiza que solo un hilo modifique la dirección de memoria a la vez[cite: 1].

### ¿Cómo se evitó bloquear JavaFX?
El hilo principal de JavaFX (*JavaFX Application Thread*) procesa de manera exclusiva la cola de eventos y el renderizado de la interfaz[cite: 1]. Si se invoca `Thread.sleep()`, bucles largos o `join()` directamente en el controlador, la UI se congela y deja de responder a eventos del sistema operativo[cite: 1]. Para evitarlo:
1. Las operaciones de las ventanillas se ejecutan en instancias independientes de `Thread`[cite: 1].
2. Los bloqueos de espera segura (`join()`) al pulsar «Detener» o «Reiniciar» se encapsulan dentro de un hilo trabajador en segundo plano.
3. Todas las mutaciones de controles visuales (etiquetas, barra de progreso y registros) se despachan al hilo gráfico mediante `Platform.runLater()`[cite: 1].

### ¿Qué condiciones se verificaron para detener y reiniciar?
Para la detención no se empleó el método obsoleto e inseguro `Thread.stop()`[cite: 1]. Se utilizó una detención cooperativa que evalúa dos condiciones en cada ciclo:
1. Una bandera `volatile boolean activo` que asegura visibilidad atómica e inmediata entre cachés de la CPU[cite: 1].
2. El estado de interrupción `Thread.currentThread().isInterrupted()`, complementado con la captura de `InterruptedException` para reactivar la señal de corte si el hilo estaba suspendido en `Thread.sleep()`[cite: 1].

Para el reinicio y reanudación, se comprobó que los hilos antiguos estuvieran completamente en estado `TERMINATED` mediante `isAlive()` y `join()`, garantizando que ninguna tarea zombi siga mutando el contador global[cite: 1].

### ¿Qué mejoraría para procesar 100,000 solicitudes?
1. **Pool de Hilos (`ExecutorService`):** Evitar la creación masiva de hilos del sistema operativo utilizando un `ThreadPoolExecutor` acotado (por ejemplo, con tamaño fijo equivalente al número de núcleos disponibles) para gestionar la cola de trabajo.
2. **Actualización Agrupada (*Batching*):** Despachar 100,000 llamadas individuales a `Platform.runLater()` colapsaría el búfer del *JavaFX Application Thread*[cite: 1]. Se debe desacoplar la tasa de procesamiento de la tasa de refresco visual, actualizando la interfaz periódicamente (por ejemplo, cada 50 o 100 transacciones acumuladas, o mediante un `AnimationTimer` sincronizado a 60 FPS)[cite: 1].

---

## 5. Retos de Análisis Colaborativo (GitHub y Git)

### ¿Qué riesgos surgen si dos integrantes modifican el mismo controlador JavaFX al mismo tiempo? Propongan una estrategia de integración.
Surgen conflictos de fusión (*merge conflicts*), regresión de funcionalidades y sobreescritura accidental de lógica crítica o inyecciones `@FXML`.
* **Estrategia:** Desacoplar responsabilidades. Modularizar la lógica de negocio en clases de servicio independientes (`tareas` y `modelo`), dejando en el controlador únicamente el enrutamiento de eventos[cite: 1]. Para ramas paralelas, se exige actualizar la rama local mediante `git fetch origin` y `git rebase origin/main` antes de abrir el Pull Request, resolviendo discrepancias de forma controlada[cite: 1].

### ¿Por qué un Pull Request con código que compila puede seguir siendo incorrecto desde el punto de vista de la concurrencia?
El compilador de Java (`javac`) solo valida la sintaxis y el sistema de tipos. No puede detectar condiciones de carrera, bloqueos mutuos (*deadlocks*), inanición de hilos (*thread starvation*), falta de visibilidad en memoria (ausencia de `volatile` o barreras de sincronización) ni violaciones del hilo gráfico por invocar métodos de JavaFX fuera del *Application Thread*.

### ¿Qué evidencias demostrarían que la detención de los hilos fue controlada y que la interfaz JavaFX no quedó bloqueada?
1. La captura de eventos de la ventana permanece fluida: la barra de progreso responde de inmediato y el botón «Iniciar/Reanudar» pasa a habilitarse al instante.
2. En la consola y en el monitor visual, el estado de los hilos transita de forma ordenada de `RUNNABLE`/`TIMED_WAITING` a `TERMINATED` sin imprimir trazas de excepción no controladas como `IllegalThreadStateException` o `InterruptedException` sin atrapar[cite: 1].
3. El depurador de hilos de IntelliJ muestra que los subprocesos `Thread-Ventanilla-A` y `Thread-Ventanilla-B` ya no existen en el árbol de hilos activos de la JVM.

### ¿Cómo se resolvería un conflicto de Git sin sobrescribir el trabajo de otro integrante? Describan y documenten el procedimiento.
1. Traer los últimos cambios validados de la rama principal:
   ```bash
   git checkout feature/mi-rama
   git fetch origin
   git merge origin/main