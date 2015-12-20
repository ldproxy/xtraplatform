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
