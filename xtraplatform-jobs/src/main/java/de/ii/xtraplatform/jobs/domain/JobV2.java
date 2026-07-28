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
import javax.annotation.Nullable;

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

  long getCreatedAt();

  long getStartedAt();

  long getUpdatedAt();

  long getFinishedAt();

  Status getStatus();

  Map<String, Object> getInputs();

  Map<String, Object> getOutputs();

  int getProgress();

  // ???
  List<String> getErrors();

  // ???
  @Nullable
  Object getDetails();
}
