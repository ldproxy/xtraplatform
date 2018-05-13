package de.ii.xsf.cfgstore.api;

import java.lang.annotation.Target;

/**
 * @author zahnen
 */
@Target({})
public @interface ConfigPropertyDescriptor {
    String name();

    String label();

    String defaultValue() default "";

    String description() default "";

    String validator() default "";
}
