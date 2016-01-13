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
