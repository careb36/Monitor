# ADR 0003 - Refactor EventBus for Scalability using Virtual Threads and ConcurrentHashMap

## English

- **Status:** Accepted
- **Date:** 2026-03-31

### Context

The previous `EventBus` implementation used `CopyOnWriteArrayList` to manage SSE client emitters and a sequential loop for broadcasting events. While functional for small sets of clients, this approach presented two critical risks for our target scale of 10,000+ concurrent clients:
1. **GC Pressure**: Every registration/deregistration triggered a full array copy, creating significant heap churn.
2. **Sequential Blocking**: Slow or disconnected clients stalled the entire broadcast pipeline, delaying critical alerts for other healthy clients.
3. **Thread Contention**: Using platform threads for I/O-intensive fan-out is not resource-efficient under high concurrency.

### Decision

We decided to refactor the `EventBus` to leverage Java 21's **Virtual Threads** and **Concurrent Data Structures**:
1. Replace `CopyOnWriteArrayList` with `ConcurrentHashMap.newKeySet()` for O(1) membership operations and weakly consistent iteration.
2. Enable global Virtual Threads in Spring Boot (`spring.threads.virtual.enabled: true`).
3. Implement a parallel fan-out broadcast where each emission task is submitted to a `VirtualThreadPerTaskExecutor`.

### Consequences

#### Positive
- **Linear Scalability**: The system easily handles 10,000+ concurrent clients with sub-second broadcast latency (verified via `EventBusStressTest`).
- **Isolation**: Slow or hung clients no longer impact the performance of other subscribers.
- **Resource Efficiency**: Virtual threads allow thousands of concurrent I/O operations with minimal memory overhead compared to platform threads.

#### Negative / Trade-offs
- **Test Complexity**: Functional tests now require asynchronous verification (e.g., `Awaitility`) because the dispatch is no longer synchronous.
- **Observability**: Standard thread dumps are more verbose when thousands of virtual threads are active (requires modern tooling for analysis).

### Alternatives Considered

- **Project Reactor / WebFlux**: Rejected because it would require a full rewrite of the service layer and introduce significant cognitive complexity for the team.
- **Netty / Direct ByteBuffers**: Rejected as "over-engineering" for our current requirements; Virtual Threads provide similar scalability with a simpler imperative programming model.

### Implementation References

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/resources/application.yml`
- `src/test/java/com/monitor/service/EventBusStressTest.java`

---

## Español

- **Estado:** Aceptado
- **Fecha:** 2026-03-31

### Contexto

La implementación previa de `EventBus` utilizaba `CopyOnWriteArrayList` para gestionar los emisores SSE y un bucle secuencial para el broadcast de eventos. Aunque funcional para pocos clientes, este enfoque presentaba riesgos críticos para nuestra escala objetivo de 10.000+ clientes concurrentes:
1. **Presión de GC**: Cada registro/desregistro disparaba una copia completa del array, saturando el heap.
2. **Bloqueo Secuencial**: Clientes lentos o desconectados frenaban todo el pipeline de despacho, retrasando alertas críticas para otros clientes saludables.
3. **Contención de Hilos**: El uso de hilos de plataforma para fan-out intensivo en I/O no es eficiente bajo alta concurrencia.

### Decisión

Decidimos refactorizar el `EventBus` para aprovechar los **Hilos Virtuales** (Virtual Threads) y **Estructuras Concurrentes** de Java 21:
1. Reemplazar `CopyOnWriteArrayList` por `ConcurrentHashMap.newKeySet()` para operaciones de membresía O(1) e iteración débilmente consistente.
2. Habilitar hilos virtuales globales en Spring Boot (`spring.threads.virtual.enabled: true`).
3. Implementar un despacho paralelo donde cada tarea de emisión se envía a un `VirtualThreadPerTaskExecutor`.

### Consecuencias

#### Positivas
- **Escalabilidad Lineal**: El sistema maneja fácilmente 10.000+ clientes con latencia de despacho de milisegundos (verificado vía `EventBusStressTest`).
- **Aislamiento**: Clientes lentos o colgados ya no impactan el rendimiento de otros suscriptores.
- **Eficiencia de Recursos**: Los hilos virtuales permiten miles de operaciones de I/O concurrentes con un overhead mínimo de memoria.

#### Negativas / Trade-offs
- **Complejidad en Tests**: Las pruebas funcionales ahora requieren verificación asincrónica (ej. `Awaitility`) porque el despacho ya no es sincrónico.
- **Observabilidad**: Los dumps de hilos estándar son más verbosos con miles de hilos virtuales activos (requiere herramientas modernas de análisis).

### Alternativas Consideradas

- **Project Reactor / WebFlux**: Rechazado porque requeriría una reescritura completa de la capa de servicios e introduciría una complejidad cognitiva significativa para el equipo.
- **Netty / Direct ByteBuffers**: Rechazado por ser "sobre-ingeniería" para los requerimientos actuales; los hilos virtuales proveen escalabilidad similar con un modelo de programación imperativo más simple.

### Referencias de Implementación

- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/resources/application.yml`
- `src/test/java/com/monitor/service/EventBusStressTest.java`
