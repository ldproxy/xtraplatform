package de.ii.xsf.core.api;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author fischer
 */
public interface StoreRegistry {
    
    List<String> getOrganizations();
    
    void deleteOrganization(String orgid) throws IOException;
}
