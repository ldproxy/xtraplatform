/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.rest;

import de.ii.xsf.core.api.Service;
import io.dropwizard.views.View;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

/**
 *
 * @author zahnen
 */
public interface ServiceResourceFactory {
    Class getServiceResourceClass();
    View getServicesView(Collection<Service> services, URI uri);
    Response getResponseForParams(Collection<Service> services, UriInfo uriInfo);
}
