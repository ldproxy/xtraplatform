package de.ii.xtraplatform.dropwizard.api;

import java.net.URI;

public interface XtraPlatform {

    XtraPlatformConfiguration getConfiguration();

    URI getUri();

    URI getServicesUri();
}
