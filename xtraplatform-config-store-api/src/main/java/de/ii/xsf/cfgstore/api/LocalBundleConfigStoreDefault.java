/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api;

import com.google.common.collect.ImmutableMap;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.core.util.json.DeepUpdater;
import de.ii.xsf.dropwizard.api.Jackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class LocalBundleConfigStoreDefault extends AbstractGenericResourceStore<JsonBundleConfig, LocalBundleConfigStore> implements LocalBundleConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalBundleConfigStoreDefault.class);
    
    public static final String STORE_ID = "local-bundle-config-store";

    private final Map<String, List<Map<String, Object>>> properties;
    private final Map<String, String> categories;
    
    //@Requires
    //ConfigurableComponentFactory coi;
    
    public LocalBundleConfigStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore) {
        super(rootConfigStore, STORE_ID, false, new DeepUpdater<>(jackson.getDefaultObjectMapper()), new JsonBundleConfigSerializer(jackson.getDefaultObjectMapper()));
        this.properties = new LinkedHashMap<>();
        this.categories = new LinkedHashMap<>();
    }

    @Override
    protected JsonBundleConfig createEmptyResource(String id, String... path) {
        return new JsonBundleConfig("",  new HashMap<>());
    }

    @Override
    protected Class<?> getResourceClass(String id, String... path) {
        return JsonBundleConfig.class;
    }

    @Validate
    public void start() {
        //coi.reconfigure();
    }

    @Override
    public void addConfigPropertyDescriptors(String category, String bundleId, String configId, Map<String, Map<String, String>> configPropertyDescriptors) {
        ensureCategoryExists(category);

        this.properties.get(category).add(ImmutableMap.<String, Object>builder()
                .put("bundleId", bundleId)
                .put("configId", configId)
                .put("___metadata___", configPropertyDescriptors)
                .build());
    }

    @Override
    public Map<String, Object> getCategories() {
        final ImmutableMap.Builder<String, Map<String, String>> metadataBuilder = ImmutableMap.builder();

        for (Map.Entry<String, String> cat: categories.entrySet()) {
            metadataBuilder.put(cat.getKey(), ImmutableMap.of("name", cat.getKey(), "label", cat.getValue()));
        }


        return ImmutableMap.<String, Object>builder()
                .put("categories", categories.keySet())
                .put("___metadata___", metadataBuilder.build())
                .build();
    }

    @Override
    public boolean hasCategory(String categoryId) {
        return categories.containsKey(categoryId);
    }

    @Override
    public Map<String, Object> getConfigProperties(String categoryId) {
        if (!categories.containsKey(categoryId) || !properties.containsKey(categories.get(categoryId))) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<String, Object> valueBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, Map<String, String>> metadataBuilder = ImmutableMap.builder();

        for (Map<String, Object> config: properties.get(categories.get(categoryId))) {
            final String bundleId = (String) config.get("bundleId");
            final String configId = (String) config.get("configId");
            final JsonBundleConfig jsonBundleConfig = this.withChild(bundleId).getResource(configId);

            valueBuilder.putAll(jsonBundleConfig.getProperties());

            final Map<String, Map<String, String>> metadata = (Map<String, Map<String, String>>) config.get("___metadata___");
            metadataBuilder.putAll(metadata);
        }

        return valueBuilder
                .put("___metadata___", metadataBuilder.build())
                .build();
    }

    // TODO: multiple BundleConfigs with the same category, update with values from different BundleConfigs

    @Override
    public void updateConfigProperties(String categoryId, Map<String, String> properties) throws IOException {
        if ((categories.containsKey(categoryId) || this.properties.containsKey(categories.get(categoryId)))
                && properties != null && !properties.isEmpty()) {
            Optional<Map<String, Object>> config = this.properties.get(categories.get(categoryId))
                                                                       .stream()
                                                                       .filter(cfg -> ((Map<String, Object>) cfg.get("___metadata___")).containsKey(properties.keySet()
                                                                                                                                                                    .iterator()
                                                                                                                                                                    .next()))
                                                                       .findFirst();
            if (config.isPresent()) {
                final String bundleId = (String) config.get().get("bundleId");
                final String configId = (String) config.get().get("configId");
                this.withChild(bundleId).updateResourceOverrides(configId, new JsonBundleConfig(configId, properties));
            }
        }
    }

    private void ensureCategoryExists(final String category) {
        final String categoryId = category.toLowerCase().replaceAll("[^A-Za-z0-9]", "_");

        if (!categories.containsKey(categoryId)) {
            this.categories.put(categoryId, category);
            this.properties.put(category, new ArrayList<>());
        }
    }
}
