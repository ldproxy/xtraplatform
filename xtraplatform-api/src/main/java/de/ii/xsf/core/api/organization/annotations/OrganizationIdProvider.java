package de.ii.xsf.core.api.organization.annotations;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import de.ii.xsf.core.api.permission.Organization;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate

@Provider
public class OrganizationIdProvider extends PerRequestTypeInjectableProvider<OrganizationId, String> {

    @Context
    HttpServletRequest request;

    public OrganizationIdProvider() {
        super(String.class);
    }
    
    
    @Override
    public Injectable<String> getInjectable(ComponentContext ic, OrganizationId a) {
        return new AbstractHttpContextInjectable<String>(){

            @Override
            public String getValue(HttpContext c) {
                return (String) request.getAttribute(Organization.class.getName());
            }

        };
    }
}
