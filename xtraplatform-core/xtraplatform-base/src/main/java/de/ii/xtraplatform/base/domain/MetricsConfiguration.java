/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.metrics.common.MetricsFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = MetricsConfiguration.class)
public class MetricsConfiguration extends MetricsFactory {}
