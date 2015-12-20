package de.ii.xsf.core.api.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author zahnen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface Auth {
    boolean required() default true;
    Role minRole() default Role.USER;
    boolean protectedResource() default false;
    String exceptions() default "http";
}
