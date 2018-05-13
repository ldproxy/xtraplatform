package de.ii.xsf.cfgstore.api;

import de.ii.xsf.configstore.api.rest.ResourceStore;

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
