/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.rest;

import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.permission.AuthorizationProvider;

/**
 *
 * @author zahnen
 */
public interface ServiceResource {
    public static final String SERVICE_TYPE_KEY = "serviceType";
    
    public Service getService();

    public void setService(Service service);
    
    public void init(AuthorizationProvider permProvider);
}
