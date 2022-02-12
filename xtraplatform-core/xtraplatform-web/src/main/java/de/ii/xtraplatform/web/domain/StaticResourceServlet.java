/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.CharMatcher;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import de.ii.xtraplatform.web.app.ResourceURL;
import de.ii.xtraplatform.web.app.amdatu.DefaultPages;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResourceServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceServlet.class);
  private static final CharMatcher SLASHES = CharMatcher.is('/');

  private static class CachedAsset {

    private final byte[] resource;
    private final String eTag;
    private final long lastModifiedTime;

    private CachedAsset(byte[] resource, long lastModifiedTime) {
      this.resource = resource;
      this.eTag = '"' + Hashing.murmur3_128().hashBytes(resource).toString() + '"';
      this.lastModifiedTime = lastModifiedTime;
    }

    public byte[] getResource() {
      return resource;
    }

    public String getETag() {
      return eTag;
    }

    public long getLastModifiedTime() {
      return lastModifiedTime;
    }
  }

  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;

  private final String resourcePath;
  private final String uriPath;
  private final Charset defaultCharset;
  private final Module module;
  private final DefaultPages defaultPages;
  private final Optional<String> rootRedirect;

  /**
   * Creates a new {@code AssetServlet} that serves static assets loaded from {@code resourceURL}
   * (typically a file: or jar: URL). The assets are served at URIs rooted at {@code uriPath}. For
   * example, given a {@code resourceURL} of {@code "file:/data/assets"} and a {@code uriPath} of
   * {@code "/js"}, an {@code AssetServlet} would serve the contents of {@code
   * /data/assets/example.js} in response to a request for {@code /js/example.js}. If a directory is
   * requested and {@code indexFile} is defined, then {@code AssetServlet} will attempt to serve a
   * file with that name in that directory. If a directory is requested and {@code indexFile} is
   * null, it will serve a 404.
   *
   * @param indexFile the filename to use when directories are requested, or null to serve no
   *     indexes
   * @param resourcePath the base URL from which assets are loaded
   * @param uriPath the URI path fragment in which all requests are rooted
   * @param defaultCharset the default character set
   * @param rootRedirect
   */
  public StaticResourceServlet(
      String resourcePath,
      String uriPath,
      Charset defaultCharset,
      Module module,
      DefaultPages defaultPages,
      Optional<String> rootRedirect) {
    final String trimmedPath = SLASHES.trimFrom(resourcePath);
    this.resourcePath = trimmedPath.isEmpty() ? trimmedPath : trimmedPath + '/';
    final String trimmedUri = SLASHES.trimTrailingFrom(uriPath);
    this.uriPath = trimmedUri.isEmpty() ? "/" : trimmedUri;
    this.defaultCharset = defaultCharset;
    this.module = module;
    this.defaultPages = defaultPages;
    this.rootRedirect = rootRedirect;
  }

  public StaticResourceServlet(
      String resourcePath, String uriPath, Charset defaultCharset, Module module) {
    this(resourcePath, uriPath, defaultCharset, module, new DefaultPages(), Optional.of("/"));
  }

  /*public URL getResourceURL() {
  return Resources.getResource(resourcePath);
  }

  public String getUriPath() {
  return uriPath;
  }

  public String getIndexFile() {
  return indexFile;
  }*/
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      final StringBuilder builder = new StringBuilder();
      // if (!req.getDispatcherType().equals(DispatcherType.FORWARD)) {
      builder.append(req.getServletPath());
      // }
      if (req.getPathInfo() != null) {
        builder.append(req.getPathInfo());
      } else if (rootRedirect.isPresent()) {
        builder.append(rootRedirect.get());
        resp.setHeader(HttpHeaders.LOCATION, builder.toString());
        resp.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
        return;
      }

      final CachedAsset cachedAsset = loadAsset(builder.toString());
      if (cachedAsset == null) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (isCachedClientSide(req, cachedAsset)) {
        resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }

      resp.setDateHeader(HttpHeaders.LAST_MODIFIED, cachedAsset.getLastModifiedTime());
      resp.setHeader(HttpHeaders.ETAG, cachedAsset.getETag());

      final String mimeTypeOfExtension = req.getServletContext().getMimeType(req.getRequestURI());
      MediaType mediaType = DEFAULT_MEDIA_TYPE;

      if (mimeTypeOfExtension != null) {
        try {
          mediaType = MediaType.parse(mimeTypeOfExtension);
          if (defaultCharset != null && mediaType.is(MediaType.ANY_TEXT_TYPE)) {
            mediaType = mediaType.withCharset(defaultCharset);
          }
        } catch (IllegalArgumentException ignore) {
        }
      }

      resp.setContentType(mediaType.type() + '/' + mediaType.subtype());

      if (mediaType.charset().isPresent()) {
        resp.setCharacterEncoding(mediaType.charset().get().toString());
      }

      try (ServletOutputStream output = resp.getOutputStream()) {
        output.write(cachedAsset.getResource());
      }
    } catch (RuntimeException | URISyntaxException ignored) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unexpected exception", ignored);
      }
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private CachedAsset loadAsset(String key) throws URISyntaxException, IOException {
    String cleanKey =
        key.startsWith("/rest/services/___static___")
            ? key.replace("/rest/services/___static___", "")
            : key;
    checkArgument(cleanKey.startsWith(uriPath));
    final String requestedResourcePath = SLASHES.trimFrom(cleanKey.substring(uriPath.length()));
    final String absoluteRequestedResourcePath =
        SLASHES.trimFrom(this.resourcePath + requestedResourcePath);

    URL requestedResourceURL = null;
    // Try to determine whether we're given a resource with an actual file, or that
    // it is pointing to an (internal) directory. In the latter case, use the default
    // pages to search instead...
    //TODO: get resources from module
    /*if (module.findEntries(absoluteRequestedResourcePath, "*", false) == null) {
      // Not a directory, may be a real file?
      requestedResourceURL = module.getResource(absoluteRequestedResourcePath);
    } else {
      // Given resource was a directory, stop looking for the actual resource
      // and check whether we can display a default page instead...
      String defaultPage = this.defaultPages.getDefaultPageFor(requestedResourcePath);
      if (!defaultPage.isEmpty()) {
        requestedResourceURL =
            module.getResource(absoluteRequestedResourcePath + '/' + defaultPage);
      }
    }*/

    if (requestedResourceURL == null) {
      return null;
    }

    long lastModified = ResourceURL.getLastModified(requestedResourceURL);

    if (lastModified < 1) {
      // Something went wrong trying to get the last modified time: just use the current time
      lastModified = System.currentTimeMillis();
    }

    // zero out the millis since the date we get back from If-Modified-Since will not have them
    lastModified = (lastModified / 1000) * 1000;
    return new CachedAsset(Resources.toByteArray(requestedResourceURL), lastModified);
  }

  private boolean isCachedClientSide(HttpServletRequest req, CachedAsset cachedAsset) {
    return cachedAsset.getETag().equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))
        && (req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) >= cachedAsset.getLastModifiedTime());
  }
}
