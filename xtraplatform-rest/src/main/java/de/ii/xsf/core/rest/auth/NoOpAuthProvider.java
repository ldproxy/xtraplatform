/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.rest.auth;

import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.Organization;
import de.ii.xsf.core.api.permission.Role;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

/**
 *
 * @author zahnen
 */

// TODO: http://www.dropwizard.io/1.2.0/docs/manual/auth.html

/*@Component
@Provides(properties = {
    @StaticServiceProperty(name = "provider.type", type = "java.lang.String", value = "auth"),
    @StaticServiceProperty(name = "service.ranking", type = "int", value = "0")
})
@Instantiate

@Provider*/
public class NoOpAuthProvider /*implements InjectableProvider<Auth, Parameter>*/ {

    /*@Context
    HttpServletRequest request;

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public AbstractHttpContextInjectable<AuthenticatedUser> getInjectable(ComponentContext ic, Auth a, Parameter c) {
        return new AbstractHttpContextInjectable<AuthenticatedUser>() {

            @Override
            public AuthenticatedUser getValue(HttpContext c) {
                AuthenticatedUser anonymous = new AuthenticatedUser();
                anonymous.setRole(Role.NONE);
                if (request != null) {
                    String orgId = (String) request.getAttribute(Organization.class.getName());
                    anonymous.setOrgId(orgId);
                }

                return anonymous;
            }
        };
    }*/
}
