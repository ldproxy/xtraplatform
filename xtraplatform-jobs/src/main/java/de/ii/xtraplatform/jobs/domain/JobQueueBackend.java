/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Optional;
import java.util.function.Function;

@AutoMultiBind
public interface JobQueueBackend extends JobQueue {
  boolean isEnabled();

  void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper);
}
