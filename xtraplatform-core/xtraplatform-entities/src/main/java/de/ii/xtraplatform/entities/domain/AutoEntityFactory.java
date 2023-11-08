/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AutoEntityFactory {

  <T extends AutoEntity> Map<String, String> check(T entityData);

  <T extends AutoEntity> Map<String, List<String>> analyze(T entityData);

  <T extends AutoEntity> T generate(
      T entityData, Map<String, List<String>> types, Consumer<Map<String, List<String>>> tracker);
}
