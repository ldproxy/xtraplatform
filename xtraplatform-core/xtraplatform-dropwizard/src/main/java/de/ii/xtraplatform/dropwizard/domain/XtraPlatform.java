package de.ii.xtraplatform.dropwizard.domain;

import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;

import java.net.URI;

public interface XtraPlatform {

    XtraPlatformConfiguration getConfiguration();

    URI getUri();

    URI getServicesUri();
}
