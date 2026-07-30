package com.example.dms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging by {@link com.example.dms.aspect.AuditAspect}.
 * The {@code entityIdExpression} and {@code detailsExpression} fields are evaluated as
 * SpEL expressions against the method's parameters (e.g. {@code "#documentId.toString()"})
 * and against the return value via {@code #result} (e.g. {@code "#result.id.toString()"}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String entityType() default "";
    String entityIdExpression() default "";    // e.g. "#documentId"
    String projectIdExpression() default "";   // e.g. "#projectId"
    String detailsExpression() default "";     // e.g. "#fileName"
}