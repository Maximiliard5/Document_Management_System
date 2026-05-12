package com.example.dms.aspect;

import com.example.dms.annotation.Audited;
import com.example.dms.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Around("@annotation(com.example.dms.annotation.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        String actor = resolveActor();

        // Build SpEL context from method parameter names + values
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        StandardEvaluationContext spelContext = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            spelContext.setVariable(paramNames[i], args[i]);
        }

        Object result = joinPoint.proceed();
        spelContext.setVariable("result", result);

        try {
            String entityId  = evaluate(audited.entityIdExpression(), spelContext, String.class);
            Long   projectId = evaluate(audited.projectIdExpression(), spelContext, Long.class);
            String details   = evaluate(audited.detailsExpression(), spelContext, String.class);

            auditService.log(actor, audited.action(), audited.entityType(),
                    entityId, projectId, details);
        } catch (Exception e) {
            log.error("Audit logging failed for action {}: {}", audited.action(), e.getMessage());
        }

        return result;
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }

    private <T> T evaluate(String expression, StandardEvaluationContext ctx, Class<T> type) {
        if (expression == null || expression.isBlank()) return null;
        return spelParser.parseExpression(expression).getValue(ctx, type);
    }
}