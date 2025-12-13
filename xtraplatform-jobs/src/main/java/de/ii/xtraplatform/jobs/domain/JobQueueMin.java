/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.Map;

public interface JobQueueMin {

  void push(BaseJob job);

  void updateJob(Job job, int progressDelta);

  void startJobSet(JobSet jobSet);

  void initJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters);

  void updateJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters);

  <T> T getJobDetails(Class<T> detailsType, Job job);

  <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet);
}
