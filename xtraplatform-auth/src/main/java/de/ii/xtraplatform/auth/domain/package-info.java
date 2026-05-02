@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {ContainerResponseFilter.class})
package de.ii.xtraplatform.auth.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import jakarta.ws.rs.container.ContainerResponseFilter;
