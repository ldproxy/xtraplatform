/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Strings;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Substitutions;
import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class SubstitutionsImpl implements Substitutions {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubstitutionsImpl.class);
  private static final String TRANSFORMER_BASE64 = "base64";

  private final Map<String, String> constants;

  @Inject
  public SubstitutionsImpl(AppContext appContext) {
    this.constants = extract(appContext.getConfiguration().getSubstitutions(), "");
  }

  public SubstitutionsImpl() {
    this.constants = Map.of();
  }

  @Override
  public StringSubstitutor getSubstitutor(boolean strict, boolean substitutionInVariables) {
    return new VariableSubstitutor(strict, substitutionInVariables);
  }

  private String lookup(String key) {
    String envKey = key.replaceAll("\\.", "_");
    String transformer = "";

    if (envKey.contains(">")) {
      transformer = envKey.substring(envKey.indexOf('>') + 1);
      envKey = envKey.substring(0, envKey.indexOf('>'));
    }

    String value = System.getenv(envKey);

    if (Objects.nonNull(value)) {
      return transform(value, transformer);
    }

    value = System.getenv(envKey.toUpperCase(Locale.ROOT));

    if (Objects.nonNull(value)) {
      return transform(value, transformer);
    }

    if (constants.containsKey(key)) {
      return constants.get(key);
    }

    return null;
  }

  private static String transform(String value, String transformer) {
    if (Strings.isNullOrEmpty(transformer)) {
      return value;
    }

    if (TRANSFORMER_BASE64.equals(transformer)) {
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    throw new IllegalArgumentException("Unknown substitution transformer: " + transformer);
  }

  public class VariableSubstitutor extends StringSubstitutor {

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public VariableSubstitutor(boolean strict, boolean substitutionInVariables) {
      super(SubstitutionsImpl.this::lookup);
      this.setEnableUndefinedVariableException(strict);
      this.setEnableSubstitutionInVariables(substitutionInVariables);
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    protected boolean substitute(TextStringBuilder buf, int offset, int length) {
      try {
        return super.substitute(buf, offset, length);
      } catch (IllegalArgumentException var5) {
        IllegalArgumentException e = var5;
        if (e.getMessage() != null && e.getMessage().contains("Cannot resolve variable")) {
          throw new UndefinedEnvironmentVariableException(e.getMessage());
        } else {
          throw e;
        }
      }
    }
  }

  private static Map<String, String> extract(Map<String, Object> constants, String prefix) {
    Map<String, String> result = new LinkedHashMap<>();

    for (Entry<String, Object> entry : constants.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.putAll(
            extract((Map<String, Object>) entry.getValue(), prefix + entry.getKey() + "."));
      } else if (entry.getValue() instanceof List) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(
              "Ignoring list value for substitution '{}'. Only scalar values and maps are supported.",
              prefix + entry.getKey());
        }
      } else {
        result.put(prefix + entry.getKey(), entry.getValue().toString());
      }
    }

    return result;
  }
}
