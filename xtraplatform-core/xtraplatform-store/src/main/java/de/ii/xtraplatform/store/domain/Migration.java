/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.store.domain.Migration.MigrationContext;
import java.util.Optional;

public interface Migration<T extends MigrationContext> {

  interface MigrationContext {}

  String getDescription();

  default Optional<String> getDescriptionDetails() {
    return Optional.empty();
  }

  boolean isApplicable(T context);
}
