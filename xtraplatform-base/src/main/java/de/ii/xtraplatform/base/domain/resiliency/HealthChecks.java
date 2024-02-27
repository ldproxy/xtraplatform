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

        Map<String, SubResult> capabilities =
            vol.getVolatileCapabilities().stream()
                .map(
                    capability ->
                        Map.entry(
                            capability,
                            new Builder()
                                .healthy(vol.isAvailable(capability))
                                .state(vol.getState(capability))
                                .message(vol.getMessage(capability).orElse(null))
                                .build()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        builder.withDetail("capabilities", capabilities);

        if (vol instanceof AbstractVolatileComposed) {
          AbstractVolatileComposed avol = ((AbstractVolatileComposed) vol);

          Map<String, SubResult> components =
              avol.getComponents().stream()
                  .map(
                      key ->
                          Map.entry(
                              key,
                              new Builder()
                                  .healthy(avol.getComponent(key).isAvailable())
                                  .state(avol.getComponent(key).getState())
                                  .message(avol.getComponent(key).getMessage().orElse(null))
                                  .build()))
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
  }
}
