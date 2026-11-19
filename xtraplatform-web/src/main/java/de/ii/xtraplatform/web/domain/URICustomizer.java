/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

/**
 * @author zahnen
 */
// this class needs that many methods, a refactoring makes no sense; the query is parsed and written
// again in overrides of the URIBuilder, so that the encoding of the commas is kept
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class URICustomizer extends URIBuilder {

  /**
   * Represents a percent-encoded comma in the value of a query parameter, while the query is
   * changed: an unencoded comma separates the items of a list, an encoded comma is a comma within
   * an item. The URIBuilder decodes both to a comma and encodes every comma when the query is
   * written again, so the query is parsed with this character for an encoded comma, and an
   * unencoded comma is written for a comma and an encoded comma for this character.
   */
  public static final char ENCODED_COMMA = '\u001F';

  private static final Pattern ENCODED_COMMA_IN_QUERY = Pattern.compile("%2[cC]");
  private static final String ENCODED_COMMA_MARKER = "%1F";
  private static final String ENCODED_COMMA_WRITTEN = "%2C";

  public URICustomizer() {
    super();
  }

  public URICustomizer(String string) throws URISyntaxException {
    super(withQuery(string, URICustomizer::markEncodedCommas));
  }

  public URICustomizer(URI uri) {
    super(
        Objects.nonNull(uri.getRawQuery())
                && ENCODED_COMMA_IN_QUERY.matcher(uri.getRawQuery()).find()
            ? URI.create(withQuery(uri.toString(), URICustomizer::markEncodedCommas))
            : uri);
  }

  @Override
  public URI build() throws URISyntaxException {
    return new URI(toString());
  }

  @Override
  public String toString() {
    return withQuery(super.toString(), URICustomizer::writeCommas);
  }

  /**
   * @return the query parameters with the decoded values, a comma within an item and a comma that
   *     separates the items are both a comma
   */
  @Override
  public List<NameValuePair> getQueryParams() {
    return super.getQueryParams().stream().map(URICustomizer::withCommas).toList();
  }

  @Override
  public NameValuePair getFirstQueryParam(String name) {
    NameValuePair param = super.getFirstQueryParam(name);
    return Objects.nonNull(param) ? withCommas(param) : null;
  }

  public URICustomizer copy() {
    try {
      return new URICustomizer(this.toString());
    } catch (URISyntaxException e) {
      // ignore
    }
    return this;
  }

  @Override
  public URICustomizer clearParameters() {
    super.clearParameters();
    return this;
  }

  @Override
  public URICustomizer setScheme(String scheme) {
    super.setScheme(scheme);
    return this;
  }

  @Override
  public URICustomizer setPathSegments(List<String> pathSegments) {
    super.setPathSegments(pathSegments);
    return this;
  }

  public URICustomizer ensureParameter(final String parameter, final String value) {
    if (super.getQueryParams().stream()
        .noneMatch(nameValuePair -> nameValuePair.getName().equals(parameter))) {
      this.setParameter(parameter, value);
    }
    return this;
  }

  public URICustomizer removeParameters(final String... parameters) {
    final List<String> parameterList = Arrays.asList(parameters);

    return removeParametersIf(parameterList::contains);
  }

  public URICustomizer removeParametersIf(final Predicate<String> name) {
    // the values as they are parsed, with the encoded commas, so that they are written again
    final List<NameValuePair> queryParams =
        super.getQueryParams().stream()
            .filter(nameValuePair -> !name.test(nameValuePair.getName()))
            .collect(Collectors.toList());

    this.setParameters(queryParams);

    return this;
  }

  // the part of a URI between '?' and '#', changed by a function
  private static String withQuery(String uri, UnaryOperator<String> change) {
    int start = uri.indexOf('?');
    if (start < 0) {
      return uri;
    }
    int end = uri.indexOf('#', start);
    if (end < 0) {
      end = uri.length();
    }
    return uri.substring(0, start + 1)
        + change.apply(uri.substring(start + 1, end))
        + uri.substring(end);
  }

  private static String markEncodedCommas(String rawQuery) {
    return ENCODED_COMMA_IN_QUERY.matcher(rawQuery).replaceAll(ENCODED_COMMA_MARKER);
  }

  // the URIBuilder writes a comma as %2C and ENCODED_COMMA as %1F
  private static String writeCommas(String rawQuery) {
    return rawQuery
        .replace(ENCODED_COMMA_WRITTEN, ",")
        .replace(ENCODED_COMMA_MARKER, ENCODED_COMMA_WRITTEN);
  }

  private static NameValuePair withCommas(NameValuePair param) {
    return Objects.nonNull(param.getValue()) && param.getValue().indexOf(ENCODED_COMMA) >= 0
        ? new BasicNameValuePair(param.getName(), param.getValue().replace(ENCODED_COMMA, ','))
        : param;
  }

  public String getLastPathSegment() {
    final List<String> pathSegments = getPathSegments();

    return pathSegments.isEmpty() ? null : pathSegments.get(pathSegments.size() - 1);
  }

  public URICustomizer prependPathSegments(final String... segments) {
    return prependPathSegments(List.of(segments));
  }

  public URICustomizer prependPathSegments(final Iterable<String> segments) {
    final List<String> pathSegments = getPathSegments();

    this.setPathSegments(
        new ImmutableList.Builder<String>().addAll(segments).addAll(pathSegments).build());

    return this;
  }

  public URICustomizer ensureLastPathSegment(final String segment) {
    final List<String> pathSegments = getPathSegments();

    if (pathSegments.isEmpty() || !pathSegments.get(pathSegments.size() - 1).equals(segment)) {
      this.setPathSegments(
          new ImmutableList.Builder<String>().addAll(pathSegments).add(segment).build());
    }
    return this;
  }

  public URICustomizer ensureLastPathSegments(final String... segments) {
    List<String> pathSegments = getPathSegments();

    if (pathSegments.size() == 1 && pathSegments.get(0).isEmpty()) {
      pathSegments = List.of();
    }

    int pathSegmentsIndex =
        pathSegments.size() >= segments.length ? pathSegments.size() - segments.length : 0;

    boolean match = true;
    int restIndex = 0;
    for (int k = pathSegmentsIndex; k < pathSegments.size(); k++) {
      for (int l = 0; k + l < pathSegments.size(); l++) {
        if (!pathSegments.get(k + l).equals(segments[l])) {
          match = false;
          break;
        }
        restIndex = l + 1;
      }
      if (match) {
        break;
      } else {
        match = true;
      }
    }

    int segmentsIndex = match ? restIndex : 0;

    this.setPathSegments(
        new ImmutableList.Builder<String>()
            .addAll(pathSegments)
            .addAll(Arrays.asList(segments).subList(segmentsIndex, segments.length))
            .build());

    optimize();

    return this;
  }

  public URICustomizer cutPathAfterSegments(final String... segments) {
    if (segments.length <= 0) {
      return this;
    }

    final List<String> pathSegments = getPathSegments();
    int lastMatchIndex = findLastMatchingSegmentIndex(pathSegments, segments);

    if (lastMatchIndex == -1) {
      return this;
    }

    int cutIndex = determineCutIndex(pathSegments, segments, lastMatchIndex);
    this.setPathSegments(
        new ImmutableList.Builder<String>().addAll(pathSegments.subList(0, cutIndex)).build());

    return this;
  }

  private int findLastMatchingSegmentIndex(List<String> pathSegments, String... segments) {
    int lastMatchIndex = -1;

    for (int i = 0; i <= pathSegments.size() - segments.length; i++) {
      if (segmentsMatchAt(pathSegments, segments, i)) {
        lastMatchIndex = i;
      }
    }

    return lastMatchIndex;
  }

  private boolean segmentsMatchAt(List<String> pathSegments, String[] segments, int startIndex) {
    for (int j = 0; j < segments.length; j++) {
      if (!pathSegments.get(startIndex + j).equals(segments[j])) {
        return false;
      }
    }
    return true;
  }

  private int determineCutIndex(List<String> pathSegments, String[] segments, int startIndex) {
    for (int k = startIndex; k < pathSegments.size(); k++) {
      for (int l = 0; k + l < pathSegments.size() && l < segments.length; l++) {
        if (!pathSegments.get(k + l).equals(segments[l])) {
          return pathSegments.size(); // No match found, return full size
        }
        if (l == segments.length - 1) {
          return k + l + 1; // Found complete match, cut after this
        }
      }
    }
    return pathSegments.size();
  }

  public URICustomizer removePathSegment(final String segment, final int index) {
    final List<String> pathSegments = getPathSegments();
    final int i = index < 0 ? pathSegments.size() + index : index;

    if (i >= 0 && i < pathSegments.size() && pathSegments.get(i).equals(segment)) {
      this.setPathSegments(
          new ImmutableList.Builder<String>()
              .addAll(pathSegments.subList(0, i))
              .addAll(pathSegments.subList(i + 1, pathSegments.size()))
              .build());
    }
    return this;
  }

  public URICustomizer removeLastPathSegment(final String segment) {
    final List<String> pathSegments = getPathSegments();

    if (!pathSegments.isEmpty() && pathSegments.get(pathSegments.size() - 1).equals(segment)) {
      this.setPathSegments(pathSegments.subList(0, pathSegments.size() - 1));
    }
    return this;
  }

  public URICustomizer removeLastPathSegments(final int numberOfSegments) {
    final List<String> pathSegments = getPathSegments();

    if (pathSegments.size() >= numberOfSegments) {
      this.setPathSegments(pathSegments.subList(0, pathSegments.size() - numberOfSegments));
    }

    return this;
  }

  public URICustomizer replaceInPath(String original, String replacement) {
    this.setPath(this.getPath().replaceFirst(original, replacement));
    return this;
  }

  public URICustomizer ensureTrailingSlash() {
    if (Objects.nonNull(this.getPath()) && !this.getPath().endsWith("/")) {
      this.setPath(this.getPath() + "/");
    }
    return this;
  }

  public URICustomizer ensureNoTrailingSlash() {
    if (Objects.nonNull(this.getPath()) && this.getPath().endsWith("/")) {
      this.setPath(this.getPath().substring(0, getPath().length() - 1));
    }
    return this;
  }
}
