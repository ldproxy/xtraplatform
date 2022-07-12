/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
@AutoMultiBind
public interface ServiceListingProvider {
  // TODO: one provider per mime type
  MediaType getMediaType();

  Response getServiceListing(List<ServiceData> services, URI uri);
}
