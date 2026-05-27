@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {ContainerResponseFilter.class})
package de.ii.xtraplatform.audit.log.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import jakarta.ws.rs.container.ContainerResponseFilter;
