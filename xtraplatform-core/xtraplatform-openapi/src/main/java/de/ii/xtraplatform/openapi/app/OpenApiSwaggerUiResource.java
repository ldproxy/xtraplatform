/*
 * Copyright 2017-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.openapi.app;

/** @author zahnen */
import com.google.common.io.Files;
import com.google.common.io.Resources;
import de.ii.xtraplatform.openapi.domain.OpenApiViewerResource;
import java.net.URL;
import java.util.Objects;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class OpenApiSwaggerUiResource implements OpenApiViewerResource {

  private static Logger LOGGER = LoggerFactory.getLogger(OpenApiSwaggerUiResource.class);

  private final BundleContext bc;

  public OpenApiSwaggerUiResource(@Context BundleContext bc) {
    this.bc = bc;
  }

  @Override
  public Response getFile(String file) {
    try {
      URL url = bc.getBundle().getResource(file);

      if (Objects.isNull(url)) {
        throw new NotFoundException();
      }

      return Response.ok((StreamingOutput) output -> Resources.asByteSource(url).copyTo(output))
          .type(getMimeType(file))
          .build();
    } catch (Throwable e) {
      throw new NotFoundException();
    }
  }

  private String getMimeType(String file) {
    switch (Files.getFileExtension(file)) {
      case "js":
        return "application/javascript";
      case "css":
        return "text/css";
      case "html":
        return "text/html";
      case "png":
        return "image/png";
      default:
        return "application/octet-stream";
    }
  }
}
