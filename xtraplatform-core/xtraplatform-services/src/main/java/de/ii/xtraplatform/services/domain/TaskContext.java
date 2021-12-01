/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

/** @author zahnen */
public interface TaskContext extends TaskProgress {

  int getMaxPartials();

  int getCurrentPartial();

  default boolean isPartial() {
    return getMaxPartials() > 1;
  }

  default boolean isFirstPartial() {
    return getCurrentPartial() == 1;
  }

  default boolean matchesPartialModulo(int number) {
    return (number % getMaxPartials()) == (getCurrentPartial() - 1);
  }

  String getThreadName();

  void pauseIfRequested();

  boolean isStopped();
}
