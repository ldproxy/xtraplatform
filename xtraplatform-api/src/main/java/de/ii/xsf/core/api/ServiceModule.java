/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api;

import de.ii.xsf.core.api.permission.AuthenticatedUser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public interface ServiceModule extends Module {

    public Service addService(AuthenticatedUser authUser, String id, Map<String, String> queryParams, File configDirectory ) throws IOException;
        
    public Service updateService(AuthenticatedUser authUser, String id, Service service);
    
    public void deleteService(AuthenticatedUser authUser, Service service);
    
    //public Service loadService(File configDirectory) throws IOException;
    
    public Service getService(AuthenticatedUser authUser, String id) throws IOException;
    
    public Map<String,List<Service>>  getServices() throws IOException;
    
    /*public Class getServiceClass();

    public Class getServiceResourceClass();

    public Class getServiceAdminResourceClass();*/

    public List<Service> getServiceList() throws Exception;
    
    public List<Service> getServiceList(AuthenticatedUser authUser);
}
