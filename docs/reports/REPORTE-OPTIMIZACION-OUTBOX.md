# Informe Técnico: Optimización y Resiliencia del Critical Outbox

**Fecha:** 31 de marzo de 2026
**Proyecto:** Monitor Dashboard (Spring Boot 4.x / Java 21)
**Autor:** Gemini CLI (Senior Architect)

---

## 1. Resumen Ejecutivo
Se realizó un análisis y posterior refactorización del componente `CriticalOutbox` para resolver riesgos críticos de **pérdida de datos** y **fugas de memoria (Memory Leaks)**. La implementación original en memoria carecía de límites físicos y políticas de auditoría, lo que comprometía la estabilidad del sistema en entornos productivos.

---

## 2. Diagnóstico del Estado Inicial
Tras la ejecución de la especificación `specs/001-revision-outbox.spec.md`, se detectaron los siguientes problemas:
- **Memory Leak:** El `InMemoryCriticalOutbox` utilizaba un `ConcurrentHashMap` que crecía indefinidamente, sin purgar eventos ya entregados.
- **Vulnerabilidad de Datos:** La implementación estaba marcada como `@Primary`, forzando el uso de RAM incluso cuando la persistencia en base de datos (JPA/Oracle) estaba disponible.
- **Falta de Auditoría:** No existían registros (logs) del ciclo de vida de los eventos críticos, incumpliendo el estándar de seguridad `SECURITY-CONFIG.md` (OWASP A09:2021).
- **Ineficiencia:** El filtrado de eventos pendientes requería iterar sobre el mapa completo ($O(N)$), impactando el rendimiento bajo carga.

---

## 3. Solución Implementada

### A. Blindaje de Memoria (Bounded Buffer)
Se implementó una política de **Evicción FIFO** (First-In, First-Out) para los eventos ya entregados:
- Se utiliza una `ConcurrentLinkedQueue` para rastrear el orden de entrega.
- Se definió un límite configurable (`monitor.outbox.in-memory.max-delivered-entries`).
- Al alcanzar el límite, los eventos entregados más antiguos se eliminan de la RAM.
- **Garantía de Seguridad:** Los eventos pendientes (`delivered: false`) **nunca** son candidatos a evicción, asegurando su reintento hasta el éxito.

### B. Optimización de Performance
- Se introdujo un `pendingIds` (Concurrent Set) para indexar únicamente los eventos que requieren atención.
- La búsqueda de eventos para reintento (`findDue`) pasó de ser una búsqueda lineal a una operación altamente eficiente sobre un set reducido de datos.

### C. Auditoría y Seguridad
Siguiendo los lineamientos de **OWASP A09**, se integró logging detallado con SLF4J:
- `INFO`: Registro de nuevo evento crítico en el Outbox.
- `INFO`: Confirmación de entrega exitosa.
- `WARN`: Fallo en entrega y programación de reintento (backoff).
- `ERROR`: Agotamiento de reintentos (Dead Letter Alert).

### D. Resiliencia en Producción
Se reconfiguró el sistema mediante **Spring Conditionals**:
- El perfil de producción (`prod`) ahora activa automáticamente el `JpaCriticalOutbox`.
- Los eventos críticos en vivo se persisten en **Oracle**, eliminando el riesgo de pérdida por reinicio del servicio.

---

## 4. Pruebas y Validación
Se realizaron dos niveles de pruebas exhaustivas:

### A. Pruebas Unitarias (InMemory)
- **Protección de RAM:** Evicción correcta de históricos al superar el límite.
- **Integridad Pendiente:** Los eventos no entregados persisten ante cualquier carga.
- **Prioridad por Severidad:** Los eventos `CRITICAL` se despachan antes que los `INFO`, independientemente del orden de llegada.
- **Agotamiento de Reintentos:** Verificación de marcado como `retry-limit-reached` tras fallos sucesivos (Dead Letter).

### B. Pruebas de Integración (JPA)
- **Persistencia Real:** Se verificó mediante `JpaCriticalOutboxIntegrationTest` que los eventos se guardan correctamente en una base de datos relacional (H2/JPA).
- **Supervivencia a Reinicios:** La arquitectura garantiza que, ante un apagado del servicio, los eventos críticos permanecen en el almacenamiento persistente para su posterior procesamiento.

---

## 5. Configuración Sugerida (application.yml)
```yaml
monitor:
  outbox:
    in-memory:
      max-delivered-entries: 500  # Ajustar según RAM disponible
    jpa:
      enabled: ${MONITOR_OUTBOX_JPA_ENABLED:false} # true en producción
```

---
**Resultado Final:** El sistema ahora cumple con los estándares de resiliencia industrial (FIFO Buffering, DB Persistence, Audit Trails), garantizando que las alertas críticas lleguen a destino sin comprometer la estabilidad del host.
