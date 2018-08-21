package de.ii.xtraplatform.service.api;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author zahnen
 */
public interface ServiceListingProvider {
    //TODO: one provider per mime type

    Response getServiceListing(List<ServiceData> services, URI uri);
    Response getStaticAsset(String path);
}
