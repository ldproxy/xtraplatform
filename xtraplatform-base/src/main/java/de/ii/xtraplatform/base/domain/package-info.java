@AutoModule(single = true, encapsulate = true)
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = {DocIgnore.class},
    get = {"is*", "get*"})
package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import org.immutables.value.Value;
import de.ii.xtraplatform.docs.DocIgnore;
