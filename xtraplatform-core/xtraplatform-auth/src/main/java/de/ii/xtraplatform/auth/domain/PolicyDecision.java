/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface PolicyDecision {

  static PolicyDecision of(
      User.PolicyDecision decision,
      Map<String, ? extends String> obligations,
      Optional<String> status) {
    return ImmutablePolicyDecision.of(decision, obligations, status);
  }

  static PolicyDecision deny() {
    return ImmutablePolicyDecision.of(User.PolicyDecision.DENY, Map.of(), Optional.empty());
  }

  @Value.Parameter
  User.PolicyDecision getDecision();

  @Value.Parameter
  Map<String, String> getObligations();

  @Value.Parameter
  Optional<String> getStatus();
}
