/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.di.domain;

import java.util.Map;
import org.apache.felix.ipojo.Factory;

public interface FactoryRegistry<T> extends Registry.State<Factory> {

  String FACTORY_FILTER_PREFIX =
      "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=";
  String FACTORY_FILTER_SUFFIX = "))";

  boolean ensureTypeExists();

  T createInstance(Map<String, Object> configuration, String... factoryProperties);

  void disposeInstance(T instance);
}
