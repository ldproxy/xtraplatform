package de.ii.xtraplatform.entity.api;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Stereotype;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Component(propagation=false)
@Provides
@HandlerDeclaration("<callback transition=\"validate\" method=\"onValidate\"></callback>")
@Stereotype
@Target(TYPE)
public @interface EntityComponent {
}
