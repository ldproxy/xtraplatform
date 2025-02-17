/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.resiliency.ImmutableSubResult.Builder;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2.State;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public interface HealthChecks {
  void register(String name, HealthCheck check);

  static HealthCheck simple(Supplier<Boolean> check) {
    return new HealthCheck() {
      @Override
      protected Result check() throws Exception {
        return check.get() ? Result.healthy() : Result.unhealthy("");
      }
    };
  }

  static HealthCheck simple(Supplier<Boolean> check, Supplier<Optional<String>> message) {
    return new HealthCheck() {
      @Override
      protected Result check() throws Exception {
        return check.get() ? Result.healthy() : Result.unhealthy(message.get().orElse(""));
      }
    };
  }

  static HealthCheck simple(Volatile2 vol) {
    return new HealthCheck() {
      @Override
      protected Result check() throws Exception {
        ResultBuilder builder = Result.builder().withDetail("state", vol.getState());

        if (vol.isAvailable()) {
          builder.healthy();
        } else {
          builder.unhealthy();
          vol.getMessage().ifPresent(builder::withMessage);
        }

        if (vol.getHealthInfo().isPresent()) {
          builder.withDetail("label", vol.getHealthInfo().get().label());
          builder.withDetail("description", vol.getHealthInfo().get().description());
          if (vol.getHealthInfo().get().hidden()) {
            builder.withDetail("hidden", true);
          }
        }

        return builder.build();
      }
    };
  }

  static HealthCheck composed(
      Supplier<Boolean> healthy,
      Supplier<State> state,
      Supplier<Optional<String>> message,
      Supplier<Set<String>> subKeys,
      Function<String, Boolean> subHealthy,
      Function<String, State> subState,
      Function<String, Optional<String>> subMessage) {
    return new HealthCheck() {
      @Override
      protected Result check() throws Exception {
        ResultBuilder builder = Result.builder().withDetail("state", state.get());

        if (healthy.get()) {
          builder.healthy();
        } else {
          builder.unhealthy();
          message.get().ifPresent(builder::withMessage);
        }

        Map<String, SubResult> components =
            subKeys.get().stream()
                .map(
                    subKey ->
                        Map.entry(
                            subKey,
                            new Builder()
                                .healthy(subHealthy.apply(subKey))
                                .state(subState.apply(subKey))
                                .message(subMessage.apply(subKey).orElse(null))
                                .build()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        builder.withDetail("components", components);

        return builder.build();
      }
    };
  }

  static HealthCheck composed(VolatileComposed vol) {
    return new HealthCheck() {
      @Override
      protected Result check() throws Exception {
        ResultBuilder builder = Result.builder().withDetail("state", vol.getState());

        if (vol.isAvailable()) {
          builder.healthy();
        } else {
          builder.unhealthy();
          vol.getMessage().ifPresent(builder::withMessage);
        }

        if (vol.getHealthInfo().isPresent()) {
          builder.withDetail("label", vol.getHealthInfo().get().label());
          builder.withDetail("description", vol.getHealthInfo().get().description());
          if (vol.getHealthInfo().get().hidden()) {
            builder.withDetail("hidden", true);
          }
        }

        Map<String, SubResult> capabilities =
            vol.getVolatileCapabilities().stream()
                .sorted()
                .map(
                    capability -> {
                      Builder capBuilder =
                          new Builder()
                              .healthy(vol.isAvailable(capability))
                              .state(vol.getState(capability))
                              .message(vol.getMessage(capability).orElse(null));

                      if (vol.getHealthInfo(capability).isPresent()) {
                        capBuilder.label(vol.getHealthInfo(capability).get().label());
                        capBuilder.description(vol.getHealthInfo(capability).get().description());
                        capBuilder.hidden(
                            vol.getHealthInfo(capability).get().hidden() ? true : null);
                      }

                      return Map.entry(capability, capBuilder.build());
                    })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        builder.withDetail("capabilities", capabilities);

        if (vol instanceof AbstractVolatileComposed) {
          AbstractVolatileComposed avol = ((AbstractVolatileComposed) vol);

          Map<String, SubResult> components =
              avol.getComponents().stream()
                  .sorted()
                  .map(
                      key -> {
                        Volatile2 comp = avol.getComponent(key);
                        Builder compBuilder =
                            new Builder()
                                .healthy(comp.isAvailable())
                                .state(comp.getState())
                                .message(comp.getMessage().orElse(null))
                                .capabilities(avol.getComponentCapabilities(key));

                        if (comp.getHealthInfo().isPresent()) {
                          compBuilder.label(comp.getHealthInfo().get().label());
                          compBuilder.description(comp.getHealthInfo().get().description());
                          compBuilder.hidden(comp.getHealthInfo().get().hidden() ? true : null);
                        }

                        return Map.entry(key, compBuilder.build());
                      })
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

          builder.withDetail("components", components);
        }

        return builder.build();
      }
    };
  }

  @Value.Immutable
  interface SubResult {
    boolean isHealthy();

    State getState();

    @JsonInclude(value = Include.NON_NULL)
    @Nullable
    String getMessage();

    @JsonInclude(value = Include.NON_NULL)
    @Nullable
    String getLabel();

    @JsonInclude(value = Include.NON_NULL)
    @Nullable
    String getDescription();

    @JsonInclude(value = Include.NON_NULL)
    @Nullable
    Boolean getHidden();

    @JsonInclude(value = Include.NON_EMPTY)
    Set<String> getCapabilities();
  }
}
