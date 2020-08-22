/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.manager.app;

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
