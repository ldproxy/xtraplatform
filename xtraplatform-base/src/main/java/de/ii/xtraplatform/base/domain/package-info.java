@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {ContainerResponseFilter.class})
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = {DocIgnore.class},
    get = {"is*", "get*"})
package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.immutables.value.Value;
import de.ii.xtraplatform.docs.DocIgnore;
