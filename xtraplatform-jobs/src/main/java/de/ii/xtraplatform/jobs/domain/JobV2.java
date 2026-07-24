/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public interface JobV2 {

  enum Status {
    ACCEPTED,
    RUNNING,
    SUCCESSFUL,
    FAILED,
    DISMISSED
  }

  String getId();

  String getType();

  AtomicLong getCreatedAt();

  AtomicLong getStartedAt();

  AtomicLong getUpdatedAt();

  AtomicLong getFinishedAt();

  Status getStatus();

  Map<String, Object> getInputs();

  Map<String, Object> getOutputs();

  int getProgress();

  // ???
  List<String> getErrors();

  // ???
  Object getDetails();
}
