package com.example.dms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String entityType() default "";
    String entityIdExpression() default "";    // e.g. "#documentId"
    String projectIdExpression() default "";   // e.g. "#projectId"
    String detailsExpression() default "";     // e.g. "#fileName"
}