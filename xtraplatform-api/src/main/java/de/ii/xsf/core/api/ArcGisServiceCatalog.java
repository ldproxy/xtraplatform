/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
