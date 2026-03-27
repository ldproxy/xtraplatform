/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ImmutableIdentifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventSource.class);

  private static final String KEY_PATTERN = "{type}/{path:**}/{id}";
  private static final Pattern PATH_PATTERN =
      Pattern.compile("(?<separator>\\/(?:[^\\/{}]+\\/)*|^)\\{(?<name>[\\w]+)(?::(?<glob>\\*+))?}");
  private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();
  private static final String TYPE_GROUP = "type";
  private static final String PATH_GROUP = "path";
  private static final String ID_GROUP = "id";
  private static final String FORMAT_GROUP = "format";

  private final Path path;

  private final Path rootPath;

  private final StoreSource source;
  private final Pattern mainPathPatternRead;
  private final String mainPathPatternWrite;
  private static final String NAME_GROUP = "name";

  public EventSource(Path path, StoreSource source, Function<String, String> pathAdjuster) {
    this.path =
        source.getContent() == Content.ALL && !source.isArchive()
            ? path.resolve(Content.ENTITIES.getPrefix())
            : path;
    this.rootPath =
        source.isArchive()
            ? source.getContent() == Content.ALL
                ? Path.of(source.getArchiveRoot()).resolve(Content.ENTITIES.getPrefix())
                : Path.of(source.getArchiveRoot())
            : path;
    this.source = source;
    this.mainPathPatternRead = pathToPattern(KEY_PATTERN, pathAdjuster);
    this.mainPathPatternWrite =
        KEY_PATTERN.replace("{type}", "%s").replace("{path:**}", "%s").replace("{id}", "%s");
  }

  public Path getPath() {
    return path;
  }

  public StoreSource getSource() {
    return source;
  }

  public Stream<EntityEvent> load(EventReader reader) {
    return getPathPatternStream()
        .flatMap(
            pathPattern -> {
              try {
                return reader
                    .load(getPath(), source.getIncludes(), source.getExcludes())
                    .map(
                        pathAndPayload ->
                            pathToEvent(
                                pathPattern, pathAndPayload.first(), pathAndPayload.second()))
                    .filter(Objects::nonNull);
              } catch (Throwable e) {
                LogContext.error(LOGGER, e, "Loading {} failed.", getSource().getLabel());
              }
              return Stream.empty();
            })
        .sorted(Comparator.naturalOrder());
  }

  public Path getSavePath(EntityEvent event) {
    return getEventPath(event.type(), event.identifier(), event.format(), mainPathPatternWrite);
  }

  public List<Path> getDeletePaths(String type, Identifier identifier, String format) {
    return Stream.of(getEventPath(type, identifier, null, mainPathPatternWrite))
        .collect(Collectors.toList());
  }

  private Path getEventPath(String type, Identifier identifier, String format, String pathPattern) {
    return rootPath.resolve(
        Paths.get(
            String.format(
                    pathPattern, type, Joiner.on('/').join(identifier.path()), identifier.id())
                + (Objects.nonNull(format) ? "." + format.toLowerCase(Locale.ROOT) : "")));
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  public EntityEvent pathToEvent(Pattern pathPattern, Path path, Supplier<byte[]> readPayload) {
    Path relPath = rootPath.relativize(path);
    Path fullRelPath = applyPrefixes(relPath);
    Matcher pathMatcher = pathPattern.matcher(fullRelPath.toString());

    if (!pathMatcher.find()) {
      return null;
    }

    String eventType = pathMatcher.group(TYPE_GROUP);
    String eventId = pathMatcher.group(ID_GROUP);
    Optional<String> eventPayloadFormat;
    try {
      eventPayloadFormat = Optional.ofNullable(pathMatcher.group(FORMAT_GROUP));
    } catch (Throwable e) {
      eventPayloadFormat = Optional.empty();
    }

    if (Objects.isNull(eventType) || Objects.isNull(eventId)) {
      return null;
    }

    String eventPath = pathMatcher.group(PATH_GROUP);

    if (!Content.isEvent(eventType)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Skipping non-event {type: {}, path: {}, id: {}}", eventType, eventPath, eventId);
      }
      return null;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Reading event {type: {}, path: {}, id: {}}", eventType, eventPath, eventId);
    }

    byte[] bytes = readPayload.get();

    Iterable<String> eventPathSegments =
        Strings.isNullOrEmpty(eventPath) ? ImmutableList.of() : PATH_SPLITTER.split(eventPath);

    return ImmutableReplayEvent.builder()
        .type(eventType)
        .identifier(ImmutableIdentifier.builder().id(eventId).path(eventPathSegments).build())
        .payload(bytes)
        .format(eventPayloadFormat.orElse(null))
        .source(source.getLabel())
        .build();
  }

  private Path applyPrefixes(Path path) {
    Path entitiesPath = Path.of(Content.ENTITIES.getPrefix());

    if (source.isSingleContent()) {
      Path contentPath =
          source.getContent() == Content.INSTANCES
              ? entitiesPath
              : Path.of(source.getContent().getPrefix());

      return contentPath.resolve(source.getPrefix().orElse("")).resolve(path);
    }

    Path resolve = path;
    // TODO
    if (source.getContent() == Content.ALL && path.startsWith(entitiesPath)) {
      resolve = entitiesPath.relativize(path);
    }
    if (resolve.startsWith(Content.INSTANCES.getPrefix())) {
      resolve = entitiesPath.resolve(resolve.subpath(1, resolve.getNameCount()));
    }

    return resolve;
  }

  public Stream<Pattern> getPathPatternStream() {
    return Stream.of(mainPathPatternRead);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private Pattern pathToPattern(String path, Function<String, String> pathAdjuster) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    StringBuilder pattern = new StringBuilder(64);
    List<String> names = new ArrayList<>();

    while (matcher.find()) {
      // LOGGER.debug("PATH REGEX {} {} {} {} {}", matcher.group(), matcher.groupCount(),
      // matcher.group(NAME_GROUP), matcher.group("separator"), matcher.group("glob"));
      if (Objects.isNull(matcher.group("glob"))) {
        names.add(matcher.group(NAME_GROUP));
        pattern.append(matcher.group("separator").replaceAll("/", "\\\\/"));
        pattern.append("(?<");
        pattern.append(matcher.group(NAME_GROUP));
        pattern.append(">[\\w][\\w-\\.]+?)");
        if (Objects.equals(matcher.group(NAME_GROUP), "id")) {
          names.add(FORMAT_GROUP);
          pattern.append("(?:\\.(?<");
          pattern.append(FORMAT_GROUP);
          pattern.append(">[\\w]+))?");
        }
      } else {
        if (!Objects.equals(matcher.group("glob"), "**")) {
          throw new IllegalArgumentException(
              "unknown store path expression: " + matcher.group("glob"));
        }
        names.add(matcher.group(NAME_GROUP));
        pattern
            .append("(?:")
            .append(matcher.group("separator").replaceAll("/", "\\\\/"))
            .append("(?<")
            .append(matcher.group(NAME_GROUP))
            .append(">(?:[\\w-_](?:[\\w-_]|\\.|\\/(?!\\.))+[\\w-_]))?)");
      }
    }
    pattern.insert(0, "^");
    pattern.append('$');

    if (!(names.contains("type") && names.contains("path") && names.contains("id"))) {
      throw new IllegalArgumentException("store path expression must contain type, path and id");
    }

    return Pattern.compile(pathAdjuster.apply(pattern.toString()));
  }
}
