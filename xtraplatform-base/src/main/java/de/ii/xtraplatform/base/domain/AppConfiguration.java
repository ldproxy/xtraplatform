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
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.logging.common.LoggingFactory;
import io.dropwizard.metrics.common.MetricsFactory;
import javax.validation.Valid;
import org.apache.commons.lang3.NotImplementedException;
import org.immutables.value.Value;

/**
 * @langEn # Configuration
 *     <p>The configuration file `cfg.yml` is located in the [Store](10-store-new.md).
 *     <p>{@docTable:properties}
 * @langDe # Konfiguration
 *     <p>Die Konfigurationsdatei `cfg.yml` befindet sich im [Store](10-store-new.md).
 *     <p>{@docTable:properties}
 * @todoEn ### Environment variables
 *     <p>Both in `cfg.yml` and in entity instances, defaults and overrides, substitutions can be
 *     made by environment variables.
 *     <p>Such an expression `${NAME}` in one of these files is replaced by the value of the
 *     environment variable `NAME`. If the variable is not set, `null` is used. You can also specify
 *     a default value in case the variable is not set. This would look like `${NAME:-VALUE}`.
 *     <p>In `cfg.yml` you can use this mechanism e.g. to define an additional store source
 *     depending on an environment variable:
 *     <p><code>
 * ```yml
 * store:
 *   sources:
 *     - type: FS
 *       src: env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>To load the files from the above example into `env/test`, you would then have to set the
 *     environment variable `DEPLOYMENT_ENV=test`. If this is not set, the directory
 *     `env/production` would be loaded.
 * @todoDe ### Umgebungsvariablen
 *     <p>Sowohl in der `cfg.yml` als auch in Entity Instanzen, Defaults und Overrides können
 *     Ersetzungen durch Umgebungsvariablen vorgenommen werden.
 *     <p>Ein solcher Ausdruck `${NAME}` in einer dieser Dateien wird durch den Wert der
 *     Umgebungsvariable `NAME` ersetzt. Ist die Variable nicht gesetzt, wird `null` eingesetzt. Man
 *     kann auch einen Default-Wert angeben, für den Fall, dass die Variable nicht gesetzt ist. Das
 *     sähe dann so `${NAME:-WERT}` aus.
 *     <p>In der `cfg.yml` kann man diesen Mechanismus z.B. verwenden, um eine zusätzliche
 *     Store-Source anhängig von einer Umgebungsvariable zu definieren:
 *     <p><code>
 * ```yml
 * store:
 *   sources:
 *     - type: FS
 *       src: env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>Um die Dateien aus obigem Beispiel in `env/test` zu laden, müsste man dann die
 *     Umgebunsvariable `DEPLOYMENT_ENV=test` setzen. Wenn diese nicht gesetzt ist würde das
 *     Verzeichnis `env/production` geladen.
 * @todoEn ### HTTP-Proxy If the application needs to use an HTTP proxy to access external
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
 * @todoDe ### HTTP-Proxy Falls die Applikation einen HTTP-Proxy verwenden muss, um auf externe
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
 * @todoEn ### Idle-Timeout This setting should only be adjusted if users report persistent problems
 *     with long-running requests. In most cases, the default setting of 30 seconds is recommended.
 * @todoDe ### Idle-Timeout Diese Einstellung sollte nur angepasst werden, falls Nutzer von
 *     anhaltenden Problemen mit langlaufenden Requests berichten. In den meisten Fällen wird die
 *     Standard-Einstellung von 30 Sekunden empfohlen.
 */
@DocFile(
    path = "application/20-configuration",
    name = "README.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {@DocStep(type = Step.JSON_PROPERTIES)},
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAppConfiguration.class)
public abstract class AppConfiguration extends Configuration {

  /**
   * @langEn See [Store](10-store-new.md).
   * @langDe Siehe [Store](10-store-new.md).
   */
  @JsonProperty("store")
  @Valid
  public abstract StoreConfiguration getStore();

  /**
   * @langEn See [Logging](20-logging.md).
   * @langDe Siehe [Logging](20-logging.md).
   */
  @JsonProperty("logging")
  @Valid
  @Override
  public abstract LoggingConfiguration getLoggingFactory();

  /**
   * @langEn See [Authorization](40-auth.md).
   * @langDe Siehe [Autorisierung](40-auth.md).
   */
  @JsonProperty("auth")
  @Valid
  public abstract AuthConfiguration getAuth();

  /**
   * @langEn See [Modules](80-modules.md).
   * @langDe Siehe [Modules](80-modules.md).
   * @since v4.0
   */
  @JsonProperty("modules")
  @Valid
  public abstract ModulesConfiguration getModules();

  /**
   * @langEn See [Background Tasks](90-background-tasks.md).
   * @langDe Siehe [Background Tasks](90-background-tasks.md).
   * @since v3.0
   */
  @JsonProperty("backgroundTasks")
  @Valid
  public abstract BackgroundTasksConfiguration getBackgroundTasks();

  @Valid
  public abstract HttpClientConfiguration getHttpClient();

  @DocIgnore
  @JsonProperty("metrics")
  @Valid
  @Override
  public abstract MetricsConfiguration getMetricsFactory();

  /**
   * @langEn See [Web Server](30-server.md).
   * @langDe Siehe [Webserver](30-server.md).
   */
  @JsonProperty("server")
  @Valid
  @Override
  public abstract ServerConfiguration getServerFactory();

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

  @JsonIgnore
  @Override
  public void setMetricsFactory(MetricsFactory factory) {
    throw new NotImplementedException();
  }
}
