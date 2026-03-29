# Monitor Operations Runbook

[English](#english) | [Espanol](#espanol)

---

<a id="english"></a>
## English

### Executive Summary

This runbook defines the minimum operating procedure to prove that `Monitor` is healthy in development or test environments.
The system should be considered operational when:

1. backend build and tests pass
2. frontend install, lint, and build pass
3. backend starts on `8080`
4. frontend starts on `3000`
5. `GET /api/events/stream` returns `:connected`
6. periodic `:heartbeat` comments are observable
7. the frontend rewrite to `/api/*` reaches the backend successfully

### Required Tooling

- Java `21`
- Maven `3.9.x` or compatible
- Node.js with npm
- optional: Docker / Docker Compose for Oracle, Zookeeper, Kafka, and Debezium

### Standard Validation

#### Backend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor"
mvn --batch-mode clean verify
```

#### Frontend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor\frontend"
npm install
npm run lint
npm run build
```

### Local Startup

#### Start backend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor"
mvn --batch-mode spring-boot:run
```

Expected behavior:
- Spring Boot starts on `8080`
- `GET /api/events/stream` is available
- the stream emits `:connected`
- the stream emits periodic `:heartbeat`

#### Start frontend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor\frontend"
npm run dev
```

Expected behavior:
- Next.js dev server starts on `3000`
- `/api/*` is rewritten to `http://localhost:8080/api/*`
- the dashboard opens the `EventSource` after user activation

### Smoke Checks

#### Backend HTTP listener

```powershell
curl.exe -I --max-time 5 http://localhost:8080/
```

A `404` response still proves that the web server is listening.

#### SSE handshake and heartbeat

```powershell
curl.exe -N --max-time 20 http://localhost:8080/api/events/stream
```

Expected output pattern:

```text
:connected

:heartbeat
```

#### Frontend root page

```powershell
curl.exe --max-time 20 http://localhost:3000/
```

Expected result:
- dashboard HTML is returned
- the UI may still show `DESCONECTADO` until the user clicks start and `EventSource.onopen` fires

#### Frontend rewrite to backend SSE

```powershell
curl.exe -N --max-time 5 http://localhost:3000/api/events/stream
```

Expected result:
- the connection opens through the Next.js rewrite
- the stream depends on backend availability

### Troubleshooting

#### Symptom: backend does not start
Check:
- `src/main/resources/application.yml`
- `MonitorMailProperties`
- `MonitorPollingTargetsProperties`
- `JacksonConfig`
- port `8080` availability

#### Symptom: frontend remains `DESCONECTADO`
Check:
- backend is running on `8080`
- frontend is running on `3000`
- `frontend/next.config.js` still contains the local rewrite
- the browser has actually triggered the start action
- `useMonitor.ts` receives `onopen`

#### Symptom: SSE opens but no business events appear
Check:
- `:connected` and `:heartbeat` are still present
- `PollingService` only emits on transition or DOWN state
- no Kafka message means no `DATA` events
- a quiet stream is not necessarily a broken stream

#### Symptom: no Kafka-driven events
Check:
- Kafka broker health
- topic name under `monitor.kafka.topic.log-traza`
- Debezium / Kafka Connect health
- Debezium payload structure (`payload.after`, `op == "c"`)

#### Symptom: no email on CRITICAL
Check:
- the event severity is actually `CRITICAL`
- producer code invokes `EmailService.sendCriticalAlert(...)`
- `monitor.mail.*` and `spring.mail.*` settings
- connectivity to the SMTP target

### Known Runtime Notes

- `EventBus` is in-memory and scoped to a single backend instance.
- `PollingService.simulatePing(...)` currently returns `true` in production code unless intentionally changed.
- the frontend only starts monitoring after a user gesture because audio must be initialized safely.
- a successful SSE connection only proves that the stream is open, not that business events are currently being generated.

### Code References

- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `src/main/resources/application.yml`
- `frontend/src/hooks/useMonitor.ts`
- `frontend/next.config.js`
- `docker-compose.yml`

---

<a id="espanol"></a>
## Espanol

### Resumen Ejecutivo

Este runbook define el procedimiento operativo minimo para demostrar que `Monitor` esta sano en entornos de desarrollo o prueba.
El sistema debe considerarse operativo cuando:

1. el build y los tests del backend pasan
2. la instalacion, lint y build del frontend pasan
3. el backend arranca en `8080`
4. el frontend arranca en `3000`
5. `GET /api/events/stream` devuelve `:connected`
6. se observan comentarios periodicos `:heartbeat`
7. el rewrite del frontend hacia `/api/*` llega correctamente al backend

### Tooling Requerido

- Java `21`
- Maven `3.9.x` o compatible
- Node.js con npm
- opcional: Docker / Docker Compose para Oracle, Zookeeper, Kafka y Debezium

### Validacion Estandar

#### Backend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor"
mvn --batch-mode clean verify
```

#### Frontend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor\frontend"
npm install
npm run lint
npm run build
```

### Arranque Local

#### Iniciar backend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor"
mvn --batch-mode spring-boot:run
```

Comportamiento esperado:
- Spring Boot arranca en `8080`
- `GET /api/events/stream` queda disponible
- el stream emite `:connected`
- el stream emite `:heartbeat` de forma periodica

#### Iniciar frontend

```powershell
Set-Location "C:\Users\careb\VisualStudio Workspace\Monitor\frontend"
npm run dev
```

Comportamiento esperado:
- Next.js arranca en `3000`
- `/api/*` se reescribe a `http://localhost:8080/api/*`
- el dashboard abre el `EventSource` despues de la activacion del usuario

### Smoke Checks

#### Listener HTTP del backend

```powershell
curl.exe -I --max-time 5 http://localhost:8080/
```

Una respuesta `404` sigue demostrando que el servidor web esta escuchando.

#### Handshake SSE y heartbeat

```powershell
curl.exe -N --max-time 20 http://localhost:8080/api/events/stream
```

Patron esperado:

```text
:connected

:heartbeat
```

#### Pagina principal del frontend

```powershell
curl.exe --max-time 20 http://localhost:3000/
```

Resultado esperado:
- se devuelve el HTML del dashboard
- la UI puede mostrar `DESCONECTADO` hasta que el usuario pulse iniciar y dispare `EventSource.onopen`

#### Rewrite del frontend hacia el SSE del backend

```powershell
curl.exe -N --max-time 5 http://localhost:3000/api/events/stream
```

Resultado esperado:
- la conexion se abre a traves del rewrite de Next.js
- el stream depende de que el backend este disponible

### Troubleshooting

#### Sintoma: el backend no arranca
Revisar:
- `src/main/resources/application.yml`
- `MonitorMailProperties`
- `MonitorPollingTargetsProperties`
- `JacksonConfig`
- disponibilidad del puerto `8080`

#### Sintoma: el frontend permanece en `DESCONECTADO`
Revisar:
- que el backend este corriendo en `8080`
- que el frontend este corriendo en `3000`
- que `frontend/next.config.js` siga conteniendo el rewrite local
- que el navegador haya disparado realmente la accion de inicio
- que `useMonitor.ts` reciba `onopen`

#### Sintoma: el SSE abre pero no aparecen eventos de negocio
Revisar:
- que sigan apareciendo `:connected` y `:heartbeat`
- que `PollingService` solo emite en transiciones o estado DOWN
- que sin mensajes Kafka no habra eventos `DATA`
- que un stream silencioso no implica necesariamente un stream roto

#### Sintoma: no llegan eventos impulsados por Kafka
Revisar:
- salud del broker Kafka
- nombre del topic en `monitor.kafka.topic.log-traza`
- salud de Debezium / Kafka Connect
- estructura del payload Debezium (`payload.after`, `op == "c"`)

#### Sintoma: no se envia correo en CRITICAL
Revisar:
- que la severidad del evento sea realmente `CRITICAL`
- que el codigo productor invoque `EmailService.sendCriticalAlert(...)`
- configuracion `monitor.mail.*` y `spring.mail.*`
- conectividad hacia el SMTP objetivo

### Notas de Runtime Conocidas

- `EventBus` es en memoria y esta acotado a una sola instancia backend.
- `PollingService.simulatePing(...)` actualmente devuelve `true` en codigo productivo salvo que se cambie intencionalmente.
- el frontend solo empieza a monitorear tras un gesto del usuario porque el audio debe inicializarse de forma segura.
- una conexion SSE exitosa solo demuestra que el stream esta abierto, no que se esten generando eventos de negocio en ese momento.

### Referencias de Codigo

- `src/main/java/com/monitor/controller/SseController.java`
- `src/main/java/com/monitor/service/EventBus.java`
- `src/main/java/com/monitor/service/KafkaConsumerService.java`
- `src/main/java/com/monitor/service/PollingService.java`
- `src/main/java/com/monitor/service/EmailService.java`
- `src/main/resources/application.yml`
- `frontend/src/hooks/useMonitor.ts`
- `frontend/next.config.js`
- `docker-compose.yml`

