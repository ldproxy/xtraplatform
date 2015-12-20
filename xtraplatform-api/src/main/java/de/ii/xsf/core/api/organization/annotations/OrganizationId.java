package de.ii.xsf.core.api.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author zahnen
 */
@Target({ ElementType.FIELD,ElementType.PARAMETER,ElementType.CONSTRUCTOR })  
@Retention(RetentionPolicy.RUNTIME)  
public @interface OrganizationId {
}
