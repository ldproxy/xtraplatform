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
import io.dropwizard.core.Configuration;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.logging.common.LoggingFactory;
import io.dropwizard.metrics.common.MetricsFactory;
import java.util.Map;
import java.util.Objects;
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
   * @langEn *Deprecated, replaced by `jobs`* See [Background Tasks](90-background-tasks.md).
   * @langDe *Deprecated, wird ersetzt durch `jobs`* Siehe [Background
   *     Tasks](90-background-tasks.md).
   * @since v3.0
   */
  @Deprecated(since = "4.6", forRemoval = true)
  @JsonProperty("backgroundTasks")
  @Valid
  public abstract BackgroundTasksConfiguration getBackgroundTasks();

  /**
   * @langEn See [Jobs](91-jobs.md).
   * @langDe Siehe [Jobs](91-jobs.md).
   * @since v4.6
   */
  @JsonProperty("jobs")
  @Valid
  public abstract JobsConfiguration getJobs();

  public int getJobConcurrency() {
    if (Objects.nonNull(getBackgroundTasks())
        && getBackgroundTasks().getMaxThreads() > 1
        && getJobs().getMaxConcurrent() == 1) {
      return getBackgroundTasks().getMaxThreads();
    }
    return getJobs().getMaxConcurrent();
  }

  /**
   * @langEn See [HTTP Client](97-http-client.md).
   * @langDe Siehe [HTTP-Client](97-http-client.md).
   */
  @JsonProperty("httpClient")
  @Valid
  public abstract HttpClientConfiguration getHttpClient();

  @DocIgnore
  @JsonProperty("metrics")
  @Valid
  @Override
  public abstract MetricsConfiguration getMetricsFactory();

  /**
   * @langEn See [Web Server](99-server.md).
   * @langDe Siehe [Webserver](99-server.md).
   */
  @JsonProperty("server")
  @Valid
  @Override
  public abstract ServerConfiguration getServerFactory();

  /**
   * @langEn See [Substitutions](95-substitutions.md).
   * @langDe Siehe [Substitutionen](95-substitutions.md).
   */
  @JsonProperty("substitutions")
  @Valid
  public abstract Map<String, Object> getSubstitutions();

  /**
   * @langEn See [Redis](110-redis.md).
   * @langDe Siehe [Redis](110-redis.md).
   * @since v4.6
   */
  @JsonProperty("redis")
  @Valid
  public abstract RedisConfiguration getRedis();

  @JsonIgnore
  @Override
  public void setServerFactory(ServerFactory factory) {
    throw new NotImplementedException();
  }

  @JsonIgnore
  @Override
  @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
  public synchronized void setLoggingFactory(LoggingFactory factory) {
    throw new NotImplementedException();
  }

  @JsonIgnore
  @Override
  public void setMetricsFactory(MetricsFactory factory) {
    throw new NotImplementedException();
  }
}
