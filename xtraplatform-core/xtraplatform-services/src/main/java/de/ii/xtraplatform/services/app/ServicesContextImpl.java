package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Strings;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.ConfigurationReader;
import de.ii.xtraplatform.services.domain.ServicesContext;
import java.net.URI;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ServicesContextImpl implements ServicesContext {

  private final URI uri;
  @Inject
  ServicesContextImpl(AppContext appContext) {
    String externalUrl = appContext.getConfiguration().getServerFactory().getExternalUrl();

    if (Strings.isNullOrEmpty(externalUrl) || Objects.equals(externalUrl, ConfigurationReader.DEFAULT_VALUE)) {
      this.uri = appContext.getUri().resolve("rest/services");
      return;
    }

    String uri = externalUrl.endsWith("/")
        ? externalUrl.substring(0, externalUrl.length() - 1)
        : externalUrl;

    this.uri = URI.create(uri);
  }

  @Override
  public URI getUri() {
    return uri;
  }
}
