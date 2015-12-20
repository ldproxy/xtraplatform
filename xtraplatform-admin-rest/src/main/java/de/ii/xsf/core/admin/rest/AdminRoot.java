package de.ii.xsf.core.admin.rest;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
public class AdminRoot {
    private String version;
    private List<String> resources;
    
    public AdminRoot(String version) {
        this.version = version;
        this.resources = new ArrayList<String>();
        resources.add("services");
        resources.add("modules");
    }
    
    public List<String> getResources() {
        return resources;
    }
    
    public String getVersion() {
        return version;
    }
}
