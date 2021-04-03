/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.ImmutableReplayEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPaths {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventPaths.class);
  private static final Pattern PATH_PATTERN =
      Pattern.compile("(?<separator>\\/(?:[^\\/{}]+\\/)*|^)\\{(?<name>[\\w]+)(?::(?<glob>\\*+))?}");
  private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();
  private static final String TYPE_GROUP = "type";
  private static final String PATH_GROUP = "path";
  private static final String ID_GROUP = "id";
  private static final String FORMAT_GROUP = "format";

  private final Path rootPath;
  private final Pattern mainPathPatternRead;
  private final String mainPathPatternWrite;
  private final List<Pattern> overridePathPatternsRead;
  private final List<String> overridePathPatternsWrite;
  private final String savePathPattern;

  public EventPaths(
      Path rootPath,
      String mainPathPattern,
      List<String> overridePathPatterns,
      Function<String, String> pathAdjuster) {
    this.rootPath = rootPath;
    this.mainPathPatternRead = pathToPattern(mainPathPattern, pathAdjuster);
    this.mainPathPatternWrite =
        mainPathPattern.replace("{type}", "%s").replace("{path:**}", "%s").replace("{id}", "%s");
    this.overridePathPatternsRead =
        overridePathPatterns.stream()
            .map((String path) -> pathToPattern(path, pathAdjuster))
            .collect(Collectors.toList());
    this.overridePathPatternsWrite =
        overridePathPatterns.stream()
            .map(
                pattern ->
                    pattern
                        .replace("{type}", "%s")
                        .replace("{path:**}", "%s")
                        .replace("{id}", "%s"))
            .collect(Collectors.toList());
    ;
    this.savePathPattern = overridePathPatternsWrite.get(overridePathPatternsWrite.size() - 1);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "STORE PATH PATTERNS: {}, {}, {}",
          this.mainPathPatternRead,
          this.overridePathPatternsRead,
          savePathPattern);
    }
  }

  public Path getRootPath() {
    return rootPath;
  }

  public Path getSavePath(EntityEvent event) {
    return getEventPath(event.type(), event.identifier(), event.format(), mainPathPatternWrite);
  }

  public List<Path> getDeletePaths(String type, Identifier identifier, String format) {
    return Stream.concat(
            overridePathPatternsWrite.stream()
                .map(pattern -> getEventPath(type, identifier, null, pattern)),
            Stream.of(getEventPath(type, identifier, null, mainPathPatternWrite)))
        .collect(Collectors.toList());
  }

  private Path getEventPath(String type, Identifier identifier, String format, String pathPattern) {
    return rootPath.resolve(
        Paths.get(
            String.format(
                    pathPattern, type, Joiner.on('/').join(identifier.path()), identifier.id())
                + (Objects.nonNull(format) ? "." + format.toLowerCase() : "")));
  }

  public EntityEvent pathToEvent(
      Pattern pathPattern, Path path, Function<Path, byte[]> readPayload) {
    int parentCount = rootPath.getNameCount();
    Matcher pathMatcher =
        pathPattern.matcher(path.subpath(parentCount, path.getNameCount()).toString());

    if (pathMatcher.find()) {
      String eventType = pathMatcher.group(TYPE_GROUP);
      String eventPath = pathMatcher.group(PATH_GROUP);
      String eventId = pathMatcher.group(ID_GROUP);
      Optional<String> eventPayloadFormat;
      try {
        eventPayloadFormat = Optional.ofNullable(pathMatcher.group(FORMAT_GROUP));
      } catch (Throwable e) {
        eventPayloadFormat = Optional.empty();
      }

      if (Objects.nonNull(eventType)
          && /*Objects.nonNull(eventPath) &&*/ Objects.nonNull(eventId)) {

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Reading event {type: {}, path: {}, id: {}}", eventType, eventPath, eventId);
        }

        byte[] bytes = readPayload.apply(path);

        Iterable<String> eventPathSegments =
            Strings.isNullOrEmpty(eventPath) ? ImmutableList.of() : PATH_SPLITTER.split(eventPath);

        return ImmutableReplayEvent.builder()
            .type(eventType)
            .identifier(ImmutableIdentifier.builder().id(eventId).path(eventPathSegments).build())
            .payload(bytes)
            .format(eventPayloadFormat.orElse(null))
            .build();
      }
    }

    return null;
  }

  public Stream<Pattern> getPathPatternStream() {
    return Stream.concat(Stream.of(mainPathPatternRead), overridePathPatternsRead.stream());
  }

  private Pattern pathToPattern(String path, Function<String, String> pathAdjuster) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    StringBuilder pattern = new StringBuilder();
    List<String> names = new ArrayList<>();

    while (matcher.find()) {
      // LOGGER.debug("PATH REGEX {} {} {} {} {}", matcher.group(), matcher.groupCount(),
      // matcher.group("name"), matcher.group("separator"), matcher.group("glob"));
      if (Objects.isNull(matcher.group("glob"))) {
        names.add(matcher.group("name"));
        pattern.append(matcher.group("separator").replaceAll("/", "\\\\/"));
        pattern.append("(?<");
        pattern.append(matcher.group("name"));
        pattern.append(">[\\w][\\w-\\.]+?)");
        if (Objects.equals(matcher.group("name"), "id")) {
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
        names.add(matcher.group("name"));
        pattern.append("(?:");
        pattern.append(matcher.group("separator").replaceAll("/", "\\\\/"));
        pattern.append("(?<");
        pattern.append(matcher.group("name"));
        pattern.append(">(?:[\\w-_](?:[\\w-_]|\\.|\\/(?!\\.))+[\\w-_]))");
        pattern.append(")?");
      }
    }
    pattern.insert(0, "^");
    pattern.append("$");

    if (!(names.contains("type") && names.contains("path") && names.contains("id"))) {
      throw new IllegalArgumentException("store path expression must contain type, path and id");
    }

    return Pattern.compile(pathAdjuster.apply(pattern.toString()));
  }
}
