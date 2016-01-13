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
package de.ii.xsf.configstore.api;

import de.ii.xsf.configstore.api.rest.MultiTenantStore;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.core.api.StoreRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author fischer
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xsf.configstore.api.rest.ResourceStore)",
        onArrival = "onModuleArrival",
        onDeparture = "onModuleDeparture")

public class StoreRegistryDefault implements StoreRegistry {

    @Context
    private BundleContext context;

    private ResourceStore orgStore;

    private final List<ResourceStore> resStores;

    public StoreRegistryDefault() {
        this.resStores = new ArrayList<>();
    }

    @Override
    public List<String> getOrganizations() {
        List<String> l = new ArrayList<>();
        if (orgStore != null) {
            l.addAll(orgStore.getResourceIds());
        }
        l.add(0, null); // add the root-org
        return l;
    }

    // delete all orgs objects. 
    // This call goes to the Cluster!
    @Override
    public void deleteOrganization(String orgid) throws IOException {
        for (ResourceStore r : resStores) {
            List<String> res = MultiTenantStore.forOrgId(r, orgid).getResourceIds();
            for (String cid : res) {
                MultiTenantStore.forOrgId(r, orgid).deleteResource(cid);
            }

        }
        orgStore.deleteResource(orgid);
    }

    public synchronized void onModuleArrival(ServiceReference<ResourceStore> ref) {
        ResourceStore mod = context.getService(ref);
        if (mod.getClass().getCanonicalName().equals("de.ii.xsf.organizations.OrganizationStoreDefault")) {
            this.orgStore = mod;
        } else {
            resStores.add(mod);
        }
    }

    public synchronized void onModuleDeparture(ServiceReference<ResourceStore> ref) {
        ResourceStore mod = context.getService(ref);

        if (mod.getClass().getCanonicalName().equals("de.ii.xsf.organizations.OrganizationStoreDefault")) {
            this.orgStore = null;
        } else {
            resStores.remove(mod);
        }

    }
}
