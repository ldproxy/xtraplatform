/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Substitutions;
import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class SubstitutionsImpl implements Substitutions {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubstitutionsImpl.class);

  private final Map<String, String> constants;

  @Inject
  public SubstitutionsImpl(AppContext appContext) {
    this.constants = extract(appContext.getConfiguration().getSubstitutions(), "");
  }

  @Override
  public StringSubstitutor getSubstitutor(boolean strict, boolean substitutionInVariables) {
    return new VariableSubstitutor(strict, substitutionInVariables);
  }

  private String lookup(String key) {
    String envKey = key.replaceAll("\\.", "_");
    String value = System.getenv(envKey);

    if (Objects.nonNull(value)) {
      return value;
    }

    value = System.getenv(envKey.toUpperCase(Locale.ROOT));

    if (Objects.nonNull(value)) {
      return value;
    }

    if (constants.containsKey(key)) {
      return constants.get(key);
    }

    return null;
  }

  public class VariableSubstitutor extends StringSubstitutor {

    public VariableSubstitutor(boolean strict, boolean substitutionInVariables) {
      super(SubstitutionsImpl.this::lookup);
      this.setEnableUndefinedVariableException(strict);
      this.setEnableSubstitutionInVariables(substitutionInVariables);
    }

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

    for (Map.Entry<String, Object> entry : constants.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.putAll(
            extract((Map<String, Object>) entry.getValue(), prefix + entry.getKey() + "."));
      } else if (entry.getValue() instanceof List) {
        LOGGER.warn(
            "Ignoring list value for substitution '{}'. Only scalar values and maps are supported.",
            prefix + entry.getKey());
      } else {
        result.put(prefix + entry.getKey(), entry.getValue().toString());
      }
    }

    return result;
  }
}
