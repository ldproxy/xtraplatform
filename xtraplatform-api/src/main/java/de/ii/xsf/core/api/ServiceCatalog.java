package de.ii.xsf.core.api;

import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public interface ServiceCatalog {

    public double getCurrentVersion();

    public List<String> getFolders();

    public List<Map<String, String>> getServices();
}
