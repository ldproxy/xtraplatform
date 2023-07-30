/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.store.domain.Migration.MigrationContext;

public interface Migration<T extends MigrationContext, U> {

  interface MigrationContext {}

  String getSubject();

  String getDescription();

  T getContext();

  boolean isApplicable(U subject);
}
