package de.ii.xsf.core.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class ArcGisServiceCatalog extends AbstractServiceCatalog {

    private final Collection<Service> services;

    public ArcGisServiceCatalog(Collection<Service> services) {
        super();
        this.services = services;
    }
    
    @Override
    public List<Map<String, String>> getServices() {
        List<Map<String, String>> srvcs = new ArrayList();
        for (Service s : services) {
            if (s.isStarted()) {
                Map<String, String> srvc = new HashMap();
                srvc.put("name", s.getId());
                srvc.put("type", s.getInterfaceSpecification());
                srvcs.add(srvc);
            }
        }
        return srvcs;
    }
    
}
