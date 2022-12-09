/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.docs.DocFile;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.logging.LoggingFactory;
import io.dropwizard.server.ServerFactory;
import javax.validation.Valid;
import org.apache.commons.lang3.NotImplementedException;
import org.immutables.value.Value;

/**
 * # Global configuration
 *
 * @langEn The configuration file `cfg.yml` is located in the data directory.
 * @langDe Die Konfigurationsdatei `cfg.yml` befindet sich im Daten-Verzeichnis.
 * @langEn ### Environment variables Both in `cfg.yml` and in configuration objects, defaults and
 *     overrides, substitutions can be made by environment variables.
 *     <p>Such an expression `${NAME}` in one of these files is replaced by the value of the
 *     environment variable `NAME`. If the variable is not set, `null` is used. You can also specify
 *     a default value in case the variable is not set. This would look like `${NAME:-VALUE}`.
 *     <p>In `cfg.yml` you can use this mechanism e.g. to define an additional directory depending
 *     on an environment variable:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>To load the files from the above example into `env/test`, you would then have to set the
 *     environment variable `DEPLOYMENT_ENV=test`. If this is not set, the directory
 *     `env/production` would be loaded.
 * @langDe ### Umgebungsvariablen Sowohl in der `cfg.yml` als auch in Konfigurationsobjekten,
 *     Defaults und Overrides können Ersetzungen durch Umgebungsvariablen vorgenommen werden.
 *     <p>Ein solcher Ausdruck `${NAME}` in einer dieser Dateien wird durch den Wert der
 *     Umgebungsvariable `NAME` ersetzt. Ist die Variable nicht gesetzt, wird `null` eingesetzt. Man
 *     kann auch einen Default-Wert angeben, für den Fall, dass die Variable nicht gesetzt ist. Das
 *     sähe dann so `${NAME:-WERT}` aus.
 *     <p>In der `cfg.yml` kann man diesen Mechanismus z.B. verwenden, um ein zusätzliches
 *     Verzeichnis anhängig von einer Umgebungsvariable zu definieren:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>Um die Dateien aus obigem Beispiel in `env/test` zu laden, müsste man dann die
 *     Umgebunsvariable `DEPLOYMENT_ENV=test` setzen. Wenn diese nicht gesetzt ist würde das
 *     Verzeichnis `env/production` geladen.
 * @langEn ### HTTP-Proxy If the application needs to use an HTTP proxy to access external
 *     resources, it can be configured as follows.
 *     <p>In this example, the HTTP proxy URL is `http://localhost:8888`. Connections to hosts
 *     listed under `nonProxyHosts` are made directly and not through the HTTP proxy. In this
 *     example, this would be `localhost`, any IP address starting with `192.168.` and any subdomain
 *     of `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 * @langDe ### HTTP-Proxy Falls die Applikation einen HTTP-Proxy verwenden muss, um auf externe
 *     Ressourcen zuzugreifen, kann dieser wie folgt konfiguriert werden.
 *     <p>In diesem Beispiel ist die HTTP-Proxy-URL `http://localhost:8888`. Verbindungen zu Hosts
 *     die unter `nonProxyHosts` augelistet sind werden direkt und nicht durch den HTTP-Proxy
 *     hergestellt. In diesem Beispiel wären das `localhost`, jede IP-Addresse die mit `192.168.`
 *     anfängt und jede Subdomain von `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 * @langEn ### Idle-Timeout This setting should only be adjusted if users report persistent problems
 *     with long-running requests. In most cases, the default setting of 30 seconds is recommended.
 * @langDe ### Idle-Timeout Diese Einstellung sollte nur angepasst werden, falls Nutzer von
 *     anhaltenden Problemen mit langlaufenden Requests berichten. In den meisten Fällen wird die
 *     Standard-Einstellung von 30 Sekunden empfohlen.
 * @see StoreConfiguration
 * @see ServerConfiguration
 * @see LoggingConfiguration
 * @see io.dropwizard.client.HttpClientConfiguration
 * @see ManagerConfiguration
 * @see BackgroundTasksConfiguration
 * @see AuthConfiguration
 */
@DocFile(path = "application/todo", name = "README.md")
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAppConfiguration.class)
public abstract class AppConfiguration extends Configuration {

  @Valid
  public abstract StoreConfiguration getStore();

  @Valid
  public abstract AuthConfiguration getAuth();

  @Valid
  public abstract ManagerConfiguration getManager();

  @Valid
  public abstract BackgroundTasksConfiguration getBackgroundTasks();

  @Deprecated(since = "3.3")
  @Valid
  public abstract ProjConfiguration getProj();

  @Valid
  public abstract HttpClientConfiguration getHttpClient();

  @JsonProperty("server")
  @Valid
  @Override
  public abstract ServerConfiguration getServerFactory();

  @JsonProperty("logging")
  @Valid
  @Override
  public abstract LoggingConfiguration getLoggingFactory();

  @JsonIgnore
  @Override
  public void setServerFactory(ServerFactory factory) {
    throw new NotImplementedException();
  }

  @JsonIgnore
  @Override
  public synchronized void setLoggingFactory(LoggingFactory factory) {
    throw new NotImplementedException();
  }

  @Deprecated(since = "3.3")
  @Value.Check
  public AppConfiguration backwardsCompatibility() {
    if (getProj().getLocation().isPresent()) {
      return new ImmutableAppConfiguration.Builder()
          .from(this)
          .proj(ModifiableProjConfiguration.create())
          .store(
              new ImmutableStoreConfiguration.Builder()
                  .from(getStore())
                  .addSources(
                      new ImmutableStoreSourceFs.Builder()
                          .typeString(Type.FS.key())
                          .content(Content.RESOURCES)
                          .src(getProj().getLocation().get())
                          .prefix("proj")
                          .build())
                  .build())
          .build();
    }

    return this;
  }
}
