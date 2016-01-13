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
package de.ii.xsf.core.rest.auth;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.InjectableProvider;
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
@Component
@Provides(properties = {
    @StaticServiceProperty(name = "provider.type", type = "java.lang.String", value = "auth"),
    @StaticServiceProperty(name = "service.ranking", type = "int", value = "0")
})
@Instantiate

@Provider
public class NoOpAuthProvider implements InjectableProvider<Auth, Parameter> {

    @Context
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
    }
}
