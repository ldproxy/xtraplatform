/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

/**
 * @author zahnen
 */
public interface SchedulerTask {

  public String getId();

  public void setId(String id);

  public String getPattern();

  public Runnable getTask();
}
