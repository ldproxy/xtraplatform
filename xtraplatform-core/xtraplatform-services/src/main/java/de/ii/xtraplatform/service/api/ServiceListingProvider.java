/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author zahnen
 */
public interface ServiceListingProvider {
    //TODO: one provider per mime type
    MediaType getMediaType();
    Response getServiceListing(List<ServiceData> services, URI uri);
    Response getStaticAsset(String path);
}
