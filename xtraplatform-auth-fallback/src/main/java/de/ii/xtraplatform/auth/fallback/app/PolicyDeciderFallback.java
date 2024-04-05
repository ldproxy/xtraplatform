/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.fallback.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.PolicyDecider;
import de.ii.xtraplatform.auth.domain.PolicyDecision;
import de.ii.xtraplatform.auth.domain.User;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PolicyDeciderFallback implements PolicyDecider {

  @Inject
  public PolicyDeciderFallback() {}

  @Override
  public PolicyDecision request(
      String resourceId,
      Map<String, Object> resourceAttributes,
      String actionId,
      Map<String, Object> actionAttributes,
      Optional<User> user) {
    return PolicyDecision.deny();
  }
}
