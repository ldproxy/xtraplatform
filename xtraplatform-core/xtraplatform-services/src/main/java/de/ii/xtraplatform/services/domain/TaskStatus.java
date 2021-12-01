/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** @author zahnen */
public interface TaskStatus {
  String getId();

  String getLabel();

  String getThreadName();

  String getStatusMessage();

  double getProgress();

  long getStartTime();

  long getEndTime();

  boolean isDone();

  void onDone(Consumer<Optional<Throwable>> runnable);

  void onChange(BiConsumer<Double, String> statusConsumer, long minInterval);

  void stop();
}
