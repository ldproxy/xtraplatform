/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
