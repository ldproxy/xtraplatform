package de.ii.xtraplatform.openapi;

import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
public interface OpenApiViewerResource {
    Response getFile(String file);
}
