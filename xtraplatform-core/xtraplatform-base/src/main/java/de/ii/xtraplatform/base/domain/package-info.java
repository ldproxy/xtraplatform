@AutoModule(single = true, encapsulate = true)
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    get = {"is*", "get*"})
package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import org.immutables.value.Value;
