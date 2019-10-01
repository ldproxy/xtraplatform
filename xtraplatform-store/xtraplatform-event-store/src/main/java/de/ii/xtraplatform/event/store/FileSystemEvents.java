package de.ii.xtraplatform.event.store;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemEvents.class);
    private static final Pattern PATH_PATTERN = Pattern.compile("(?<separator>\\/(?:[^\\/{}]+\\/)*|^)\\{(?<name>[\\w]+)(?::(?<glob>\\*+))?}");
    private static final Splitter PATH_SPLITTER = Splitter.on('/')
                                                          .omitEmptyStrings();
    private static final String TYPE_GROUP = "type";
    private static final String PATH_GROUP = "path";
    private static final String ID_GROUP = "id";

    // {store}/{type}/{id} -> (?<store>[\w-]+)\/(?<type>[\w-]+)\/(?<id>[\w-]+)

    // {store}/{path:**}/{id} -> (?<type>[\w-]+)(?:\/(?<path>[\w-\/]+))?\/(?<id>[\w-]+)

    // {store}/{path:**}/#overrides#/{id} -> (?<type>[\w-]+)(?:\/(?<path>[\w-\/]+))?\/#overrides#\/(?<id>[\w-]+)

    private final Path rootPath;
    private final Pattern mainPathPatternRead;
    private final String mainPathPatternWrite;
    private final List<Pattern> overridePathPatternsRead;
    private final List<String> overridePathPatternsWrite;
    private final String savePathPattern;

    public FileSystemEvents(Path rootPath, String mainPathPattern, List<String> overridePathPatterns) {
        this.rootPath = rootPath;
        this.mainPathPatternRead = pathToPattern(mainPathPattern);
        this.mainPathPatternWrite = mainPathPattern.replace("{type}", "%s").replace("{path:**}", "%s").replace("{id}", "%s");
        this.overridePathPatternsRead = overridePathPatterns.stream()
                                                            .map(this::pathToPattern)
                                                            .collect(Collectors.toList());
        this.overridePathPatternsWrite = overridePathPatterns.stream()
                                                             .map(pattern -> pattern.replace("{type}", "%s").replace("{path:**}", "%s").replace("{id}", "%s"))
                                                             .collect(Collectors.toList());;
        this.savePathPattern = overridePathPatternsWrite.get(overridePathPatternsWrite.size()-1);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("STORE PATH PATTERNS: {}, {}, {}", this.mainPathPatternRead, this.overridePathPatternsRead, savePathPattern);
        }
    }

    public Stream<MutationEvent> loadEventStream() {

        return getPatternStream().flatMap(this::loadEventStream);
    }

    public void saveEvent(MutationEvent event) throws IOException {

        //TODO: check mainPath first, if exists use override
        //TODO: if override exists, merge with incoming
        Path eventPath = getEventFilePath(event.type(), event.identifier(), mainPathPatternWrite);
        if (Files.exists(eventPath)) {
            eventPath = getEventFilePath(event.type(), event.identifier(), savePathPattern);
        }
        Files.createDirectories(eventPath.getParent());
        Files.write(eventPath, event.payload());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Saved event to file {}", eventPath);
        }
    }

    public void deleteAllEvents(String type, Identifier identifier) throws IOException {
        for (String pattern : overridePathPatternsWrite) {
            Path eventPath = getEventFilePath(type, identifier, pattern);
            boolean deleted = Files.deleteIfExists(eventPath);
            if (LOGGER.isDebugEnabled() && deleted) {
                LOGGER.debug("Deleted event file {}", eventPath);
            }
        }
        Path eventPath = getEventFilePath(type, identifier, mainPathPatternWrite);
        boolean deleted = Files.deleteIfExists(eventPath);
        if (LOGGER.isDebugEnabled() && deleted) {
            LOGGER.debug("Deleted event file {}", eventPath);
        }
    }

    private Path getEventFilePath(String type, Identifier identifier, String pathPattern) {
        return rootPath.resolve(Paths.get(String.format(pathPattern, type, Joiner.on('/').join(identifier.path()), identifier.id())));
    }

    private Stream<MutationEvent> loadEventStream(Pattern pathPattern) {
        if (Objects.isNull(rootPath) || !Files.exists(rootPath)) {
            throw new IllegalArgumentException("Store path does not exist");
        }

        int parentCount = rootPath.getNameCount();

        //TODO: 3 depends on pattern
        try {
            return Files.find(rootPath, 32, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile() && path.getNameCount() - parentCount >= 3)
                        .map(path -> {
                            Matcher matcher = pathPattern.matcher(path.subpath(parentCount, path.getNameCount())
                                                                      .toString());

                            return pathToEvent(path, matcher);
                        })
                        .filter(Objects::nonNull);
        } catch (IOException e) {
            throw new IllegalStateException("Reading event from store path failed", e);
        }

    }

    private MutationEvent pathToEvent(Path path, Matcher pathMatcher) {
        if (pathMatcher.find()) {
            String eventType = pathMatcher.group(TYPE_GROUP);
            String eventPath = pathMatcher.group(PATH_GROUP);
            String eventId = pathMatcher.group(ID_GROUP);

            if (Objects.nonNull(eventType) && Objects.nonNull(eventPath) && Objects.nonNull(eventId)) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Reading event {type: {}, path: {}, id: {}}", eventType, eventPath, eventId);
                }

                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Reading event from file failed", e);
                }

                return ImmutableMutationEvent.builder()
                                             .type(eventType)
                                             .identifier(ImmutableIdentifier.builder()
                                                                            .id(eventId)
                                                                            .path(PATH_SPLITTER.split(eventPath))
                                                                            .build())
                                             .payload(bytes)
                                             .build();
            }
        }

        return null;
    }

    private Stream<Pattern> getPatternStream() {
        return Stream.concat(Stream.of(mainPathPatternRead), overridePathPatternsRead.stream());
    }

    private Pattern pathToPattern(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);
        StringBuilder pattern = new StringBuilder();
        List<String> names = new ArrayList<>();

        while (matcher.find()) {
            //LOGGER.debug("REGEX {} {} {} {} {}", matcher.group(), matcher.groupCount(), matcher.group("name"), matcher.group("separator"), matcher.group("glob"));
            if (Objects.isNull(matcher.group("glob"))) {
                names.add(matcher.group("name"));
                pattern.append(matcher.group("separator").replaceAll("/", "\\\\/"));
                pattern.append("(?<");
                pattern.append(matcher.group("name"));
                pattern.append(">[\\w-]+)");
            } else {
                if (!Objects.equals(matcher.group("glob"), "**")) {
                    throw new IllegalArgumentException("unknown store path expression: " + matcher.group("glob"));
                }
                names.add(matcher.group("name"));
                pattern.append("(?:");
                pattern.append(matcher.group("separator").replaceAll("/", "\\\\/"));
                pattern.append("(?<");
                pattern.append(matcher.group("name"));
                pattern.append(">[\\w-\\/]+)");
                pattern.append(")?");
            }
        }

        if (!(names.contains("type") && names.contains("path") && names.contains("id"))) {
            throw new IllegalArgumentException("store path expression must contain type, path and id");
        }

        return Pattern.compile(pattern.toString().replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator()));
    }
}
