/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cfgstore.api;

import de.ii.xtraplatform.kvstore.api.rest.ResourceStore;

import java.io.IOException;
import java.util.Map;

/**
 * @author zahnen
 */
public interface BundleConfigStore extends ResourceStore<JsonBundleConfig> {
    void addConfigPropertyDescriptors(String category, String bundleId, String configId, Map<String, Map<String, String>> configPropertyDescriptors);
    Map<String, Object> getCategories();
    boolean hasCategory(String categoryId);
    Map<String, Object> getConfigProperties(String categoryId);
    void updateConfigProperties(String categoryId, Map<String, String> properties) throws IOException;
}
