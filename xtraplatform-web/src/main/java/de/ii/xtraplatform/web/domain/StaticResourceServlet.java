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
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import de.ii.xtraplatform.web.domain.StaticResourceReader.CachedResource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResourceServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceServlet.class);
  private static final CharMatcher SLASHES = CharMatcher.is('/');
  private static final String DEFAULT_PAGE = "index.html";

  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;

  private final String resourcePath;
  private final String uriPath;
  private final Charset defaultCharset;
  private final StaticResourceReader resourceReader;
  private final transient Optional<String> rootRedirect;
  private final Set<String> noCacheExtensions;

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
   * @param resourcePath the base URL from which assets are loaded
   * @param uriPath the URI path fragment in which all requests are rooted
   * @param defaultCharset the default character set
   * @param resourceReader
   * @param rootRedirect
   */
  public StaticResourceServlet(
      String resourcePath,
      String uriPath,
      Charset defaultCharset,
      StaticResourceReader resourceReader,
      Set<String> noCacheExtensions,
      Optional<String> rootRedirect) {
    super();
    final String trimmedPath = SLASHES.trimFrom(resourcePath);
    this.resourcePath = trimmedPath.isEmpty() ? trimmedPath : trimmedPath + '/';
    final String trimmedUri = SLASHES.trimTrailingFrom(uriPath);
    this.uriPath = trimmedUri.isEmpty() ? "/" : trimmedUri;
    this.defaultCharset = defaultCharset;
    this.resourceReader = resourceReader;
    this.rootRedirect = rootRedirect;
    this.noCacheExtensions = noCacheExtensions;
  }

  public StaticResourceServlet(
      String resourcePath,
      String uriPath,
      Charset defaultCharset,
      StaticResourceReader resourceReader,
      Set<String> noCacheExtensions) {
    this(
        resourcePath, uriPath, defaultCharset, resourceReader, noCacheExtensions, Optional.of("/"));
  }

  public StaticResourceServlet(
      String resourcePath,
      String uriPath,
      Charset defaultCharset,
      StaticResourceReader resourceReader) {
    this(resourcePath, uriPath, defaultCharset, resourceReader, Set.of(), Optional.of("/"));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      String assetPath = buildAssetPath(req, resp);
      if (assetPath == null) {
        return; // Response already sent by buildAssetPath
      }

      final CachedResource cachedAsset = loadAsset(assetPath);
      if (cachedAsset == null) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (isCachedClientSide(req, cachedAsset)) {
        resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }

      configureResponse(req, resp, assetPath, cachedAsset);
      writeResponse(resp, cachedAsset);
    } catch (RuntimeException | URISyntaxException ignored) {
      handleException(resp, ignored);
    }
  }

  private String buildAssetPath(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    final StringBuilder builder = new StringBuilder();
    builder.append(req.getServletPath());

    if (req.getPathInfo() != null) {
      builder.append(req.getPathInfo());
    } else if (rootRedirect.isPresent()) {
      builder.append(rootRedirect.get());
      resp.setHeader(HttpHeaders.LOCATION, builder.toString());
      resp.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
      return null;
    }

    return builder.toString();
  }

  private void configureResponse(
      HttpServletRequest req,
      HttpServletResponse resp,
      String assetPath,
      CachedResource cachedAsset) {
    if (assetPath.contains(".")
        && noCacheExtensions.contains(assetPath.substring(assetPath.lastIndexOf('.') + 1))) {
      resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    }

    resp.setDateHeader(HttpHeaders.LAST_MODIFIED, cachedAsset.getLastModified());
    resp.setHeader(HttpHeaders.ETAG, cachedAsset.getETag());

    setContentType(req, resp);
  }

  private void setContentType(HttpServletRequest req, HttpServletResponse resp) {
    final String mimeTypeOfExtension = req.getServletContext().getMimeType(req.getRequestURI());
    MediaType mediaType = DEFAULT_MEDIA_TYPE;

    if (mimeTypeOfExtension != null) {
      try {
        mediaType = MediaType.parse(mimeTypeOfExtension);
        if (defaultCharset != null && mediaType.is(MediaType.ANY_TEXT_TYPE)) {
          mediaType = mediaType.withCharset(defaultCharset);
        }
      } catch (IllegalArgumentException ignore) {
        // Keep default media type
      }
    }

    resp.setContentType(mediaType.type() + '/' + mediaType.subtype());
    if (mediaType.charset().isPresent()) {
      resp.setCharacterEncoding(mediaType.charset().get().toString());
    }
  }

  private void writeResponse(HttpServletResponse resp, CachedResource cachedAsset)
      throws IOException {
    try (ServletOutputStream output = resp.getOutputStream()) {
      output.write(cachedAsset.getResource());
    }
  }

  private void handleException(HttpServletResponse resp, Exception ignored) throws IOException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Unexpected exception", ignored);
    }
    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  private CachedResource loadAsset(String key) throws URISyntaxException, IOException {
    String cleanKey =
        key.contains(StaticResourceHandler.PREFIX)
            ? key.substring(
                key.indexOf(StaticResourceHandler.PREFIX) + StaticResourceHandler.PREFIX.length())
            : key;
    try {
      checkArgument(cleanKey.startsWith(uriPath));
    } catch (IllegalArgumentException e) {
      return null;
    }
    final String requestedResourcePath = SLASHES.trimFrom(cleanKey.substring(uriPath.length()));
    final String absoluteRequestedResourcePath =
        "/" + SLASHES.trimFrom(this.resourcePath + requestedResourcePath);

    return resourceReader
        .load(absoluteRequestedResourcePath, Optional.of(DEFAULT_PAGE))
        .orElse(null);
  }

  public Optional<CachedResource> getAsset(String key) {
    try {
      return Optional.ofNullable(loadAsset(key));
    } catch (Throwable e) {
      return Optional.empty();
    }
  }

  private boolean isCachedClientSide(HttpServletRequest req, CachedResource cachedAsset) {
    // HTTP: A recipient MUST ignore If-Modified-Since if the request contains an If-None-Match
    // header field
    String ifNoneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);
    String eTag = cachedAsset.getETag();
    if (Objects.nonNull(ifNoneMatch) && Objects.nonNull(eTag)) {
      return eTag.equals(ifNoneMatch);
    }
    // HTTP: A recipient MUST ignore the If-Modified-Since header field if the request method is
    // neither GET nor HEAD.
    String method = req.getMethod().toUpperCase(Locale.ROOT);
    if ("GET".equals(method) || "HEAD".equals(method)) {
      try {
        long ifModifiedSince = req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        return ifModifiedSince >= cachedAsset.getLastModified();
      } catch (IllegalArgumentException e) {
        // HTTP: A recipient MUST ignore the If-Modified-Since header field if the received
        // field-value is not a valid HTTP-date
      }
    }
    return false;
  }
}
