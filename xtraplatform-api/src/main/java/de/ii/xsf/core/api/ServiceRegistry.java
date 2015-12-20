package de.ii.xsf.core.api;

import de.ii.xsf.core.api.permission.AuthenticatedUser;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public interface ServiceRegistry {
    public List<Map<String, String>> getServiceTypes();
    public Collection<Service> getServices(AuthenticatedUser authUser);
    public void addService(AuthenticatedUser authUser, String type, String id, Map<String, String> params);
    public void updateService(AuthenticatedUser authUser, String id, Service update);
    public void deleteService(AuthenticatedUser authUser, Service service);
    public Service getService(AuthenticatedUser authUser, String id);
}
