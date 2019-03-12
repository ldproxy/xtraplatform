/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.api.permission.AuthorizationProvider;
import de.ii.xtraplatform.entity.api.EntityRepository;
import io.dropwizard.views.ViewRenderer;

/**
 *
 * @author zahnen
 */
public interface ServiceResource {
    public static final String SERVICE_TYPE_KEY = "serviceType";
    
    public ServiceData getService();

    public void setService(ServiceData service);
    
    public void init(ObjectMapper defaultObjectMapper, EntityRepository entityRepository, AuthorizationProvider permProvider);

    void setMustacheRenderer(ViewRenderer mustacheRenderer);
}
