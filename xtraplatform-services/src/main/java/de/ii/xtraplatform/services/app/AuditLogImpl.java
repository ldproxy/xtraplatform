/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuditLogConfiguration;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.services.domain.AuditLog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogImpl implements AuditLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogImpl.class);
  private final Map<String, Log> auditLogMapping = new ConcurrentHashMap<>();
  private final ResourceStore auditLogStore;
  private final AppContext appContext;
  private ObjectWriter objectWriter;

  @Inject
  AuditLogImpl(Jackson jackson, ResourceStore resourceStore, AppContext appContext) {
    objectWriter =
        jackson
            .getDefaultObjectMapper()
            .writer()
            .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    if (appContext.getConfiguration().getAuditLog().getType() == AuditLogConfiguration.TYPE.JSON) {
      objectWriter = objectWriter.without(SerializationFeature.INDENT_OUTPUT);
    } else {
      objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);
    }

    this.auditLogStore = resourceStore.writableWith("logs", "audit");
    this.appContext = appContext;
  }

  private Optional<Log> getOptionalLog(String requestId) {
    if (auditLogMapping.containsKey(requestId)) {
      return Optional.of(auditLogMapping.get(requestId));
    } else {
      LOGGER.error("No AuditLog-object found for requestId {}", requestId);
      return Optional.empty();
    }
  }

  private boolean isDisabled() {
    return !isEnabled();
  }

  private Path createPath(String requestId, Log log) {
    String pathPrefix = appContext.getConfiguration().getAuditLog().getPathPrefix();

    // Replace api
    String api = Objects.isNull(log.getApi()) ? "landingpage" : log.getApi();
    pathPrefix = pathPrefix.replace("{api}", api);

    // Replace date
    String isoDate =
        DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(log.getStarted());
    pathPrefix = pathPrefix.replace("{date}", isoDate);

    return Path.of(pathPrefix).resolve(Path.of(requestId + ".json"));
  }

  private Map<String, Object> filterClaims(Map<String, Object> claims) {
    Map<String, Object> filteredClaims = new LinkedHashMap<>();
    List<String> included = appContext.getConfiguration().getAuditLog().getClaims().getIncluded();
    List<String> excluded = appContext.getConfiguration().getAuditLog().getClaims().getExcluded();

    claims.forEach(
        (k, v) -> {
          if (!excluded.contains("*")
              && !excluded.contains(k)
              && (included.contains("*") || included.contains(k))) {
            filteredClaims.put(k, v);
          }
        });

    return filteredClaims;
  }

  @Override
  public void createLog(String requestId) {
    if (isDisabled()) {
      return;
    }
    auditLogMapping.computeIfAbsent(requestId, k -> new LogImpl(requestId));
  }

  @Override
  public void abortLog(String requestId) {
    auditLogMapping.remove(requestId);
  }

  @Override
  public void setIncludePropertyValues(String requestId, boolean value) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setIncludePropertyValues(value));
  }

  @Override
  public boolean getIncludePropertyValues(String requestId) {
    return getOptionalLog(requestId).map(Log::getIncludePropertyValues).orElse(false);
  }

  @Override
  public boolean logIsAvailable(String requestId) {
    return !isDisabled() && auditLogMapping.containsKey(requestId);
  }

  @Override
  public boolean isEnabled() {
    return appContext.getConfiguration().getAuditLog().getEnabled();
  }

  @Override
  public void setApi(String requestId, String api) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setApi(api));
  }

  @Override
  public void setActor(
      String requestId, String actorType, String actorId, Map<String, Object> claims) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId)
        .ifPresent(log -> log.setActor(actorType, actorId, filterClaims(claims)));
  }

  @Override
  public void setOperationMethod(String requestId, String method) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setOperationMethod(method));
  }

  @Override
  public void setOperationPath(String requestId, String path) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setOperationPath(path));
  }

  @Override
  public void setOperationHeaders(String requestId, MultivaluedMap<String, String> headers) {
    if (isDisabled()) {
      return;
    }

    List<String> included = appContext.getConfiguration().getAuditLog().getHeaders().getIncluded();
    List<String> excluded = appContext.getConfiguration().getAuditLog().getHeaders().getExcluded();
    MultivaluedMap<String, String> headersFiltered = new MultivaluedHashMap<>();

    headers.forEach(
        (k, v) -> {
          if (!excluded.contains("*")
              && !excluded.contains(k)
              && (included.contains("*") || included.contains(k))) {
            headersFiltered.addAll(k, v);
          }
        });

    getOptionalLog(requestId).ifPresent(log -> log.setOperationHeaders(headersFiltered));
  }

  @Override
  public void setOperationStatus(String requestId, String status) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setOperationStatus(status));
  }

  @Override
  public void setTarget(String requestId, Map<String, Object> target) {
    if (isDisabled()) {
      return;
    }
    getOptionalLog(requestId).ifPresent(log -> log.setTarget(target));
  }

  @Override
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  public boolean removeAndWriteLog(String requestId) {
    if (isDisabled()) {
      return false;
    }

    Log log = auditLogMapping.remove(requestId);
    if (Objects.isNull(log)) {
      LOGGER.error("No AuditLog-object found for requestId {}", requestId);
      return false;
    }

    log.finish();

    final ByteArrayInputStream inputStream;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(objectWriter.writeValueAsString(log));
      }
      inputStream = new ByteArrayInputStream(objectWriter.writeValueAsBytes(log));
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize log {}", requestId, e);
      return false;
    }

    Path path = createPath(requestId, log);
    int maxRetries = appContext.getConfiguration().getAuditLog().getRetries();
    int retries = 0;
    do {
      try {
        inputStream.reset();
        auditLogStore.put(path, inputStream);
        return true;
      } catch (IOException e) {
        // ToDo: Delay until next try
        if (retries < maxRetries) {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Failed to write audit log {}, retrying...", requestId);
          }
        } else {
          LOGGER.error("Giving up writing audit log {} after {} retries", requestId, retries, e);
          return false;
        }
      }
      retries++;
    } while (retries <= maxRetries);

    return false;
  }

  @JsonPropertyOrder({"id", "started", "finished", "api", "actor", "operation", "target"})
  public static class LogImpl implements Log {
    private final String id;
    private final Instant started;
    private final Map<String, Object> actor = new LinkedHashMap<>();
    private final Map<String, Object> operation = new LinkedHashMap<>();
    private Instant finished;
    private Map<String, Object> target;
    private String api;
    private boolean includePropertyValues = true;

    LogImpl(String id) {
      this.id = id;
      this.started = Instant.now();
    }

    @Override
    public void finish() {
      finished = Instant.now();
    }

    @Override
    public void setActor(String actorType, String actorId, Map<String, Object> claims) {
      actor.put("type", actorType);
      actor.put("id", actorId);
      actor.put("claims", claims);
    }

    @Override
    public void setOperationMethod(String method) {
      operation.put("method", method);
    }

    @Override
    public void setOperationPath(String path) {
      operation.put("path", path);
    }

    @Override
    public void setOperationHeaders(MultivaluedMap<String, String> headers) {
      Map<String, Object> headersReduced = new LinkedHashMap<>();

      headers.forEach(
          (key, values) -> {
            if (!values.isEmpty()) {
              headersReduced.put(key, values.size() == 1 ? values.get(0) : values);
            }
          });

      operation.put("headers", headersReduced);
    }

    @Override
    public void setOperationStatus(String status) {
      operation.put("status", status);
    }

    @JsonIgnore
    @Override
    public boolean getIncludePropertyValues() {
      return includePropertyValues;
    }

    @Override
    public void setIncludePropertyValues(boolean value) {
      includePropertyValues = value;
    }

    @JsonProperty("id")
    @Override
    public String getId() {
      return id;
    }

    @JsonProperty("started")
    @Override
    public Instant getStarted() {
      return started;
    }

    @JsonProperty("finished")
    @Override
    public Instant getFinished() {
      return finished;
    }

    @JsonProperty("api")
    @Override
    public String getApi() {
      return api;
    }

    @Override
    public void setApi(String api) {
      this.api = api;
    }

    @JsonProperty("actor")
    @Override
    public Map<String, Object> getActor() {
      // Apply the anonymous user if no actor has been set
      if (!actor.containsKey("type") && !actor.containsKey("id")) {
        actor.put("type", "AnonymousUser");
        actor.put("id", "Anonymous");
      }

      return actor;
    }

    @JsonProperty("operation")
    @Override
    public Map<String, Object> getOperation() {
      return operation;
    }

    @JsonProperty("target")
    @Override
    public Map<String, Object> getTarget() {
      return target;
    }

    @Override
    public void setTarget(Map<String, Object> target) {
      this.target = new LinkedHashMap<>(target);
    }
  }
}
