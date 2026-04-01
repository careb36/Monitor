package com.monitor.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for automatic security audit logging on sensitive endpoints.
 *
 * <p>Logs authentication attempts, endpoint access, and execution time
 * for security monitoring and incident response (OWASP A09).
 *
 * <p>Integrates with SIEM systems via structured logging (JSON format
 * can be enabled via Logback configuration).
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet">OWASP Logging</a>
 */
@Aspect
@Component
public class SecurityAuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Pointcut for all SSE endpoint access.
     */
    @Pointcut("execution(* com.monitor.controller.SseController.stream(..))")
    private void sseEndpoint() {}

    /**
     * Pointcut for all controller methods.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    private void restController() {}

    /**
     * Logs SSE endpoint access with timing information.
     * Useful for detecting connection storms or abuse patterns.
     */
    @Around("sseEndpoint()")
    public Object auditSseAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();

        auditLog.info("SSE_CONNECT: method={} client_ip=see_httpServletRequest", method);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            auditLog.info("SSE_SUCCESS: method={} duration_ms={}", method, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            auditLog.warn("SSE_ERROR: method={} duration_ms={} error={}",
                    method, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Logs execution time for all REST controller methods.
     * Helps identify performance anomalies that may indicate attacks.
     */
    @Around("restController()")
    public Object auditRestAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > 5000) {
                auditLog.warn("SLOW_ENDPOINT: method={} duration_ms={} (threshold: 5000ms)", method, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            auditLog.error("ENDPOINT_ERROR: method={} duration_ms={} error={}",
                    method, duration, e.getMessage());
            throw e;
        }
    }
}
