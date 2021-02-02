package de.ii.xtraplatform.event.store;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.api.functional.LambdaWithException.consumerMayThrow;

public class FileSystemEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemEvents.class);
    private static final Pattern PATH_PATTERN = Pattern.compile("(?<separator>\\/(?:[^\\/{}]+\\/)*|^)\\{(?<name>[\\w]+)(?::(?<glob>\\*+))?}");
    private static final Splitter PATH_SPLITTER = Splitter.on('/')
                                                          .omitEmptyStrings();
    private static final String TYPE_GROUP = "type";
    private static final String PATH_GROUP = "path";
    private static final String ID_GROUP = "id";
    private static final String FORMAT_GROUP = "format";

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
        Path eventPath = getEventFilePath(event.type(), event.identifier(), event.format(), mainPathPatternWrite);
        /*if (Files.exists(eventPath)) {
            eventPath = getEventFilePath(event.type(), event.identifier(), event.format(), savePathPattern);
        }*/
        Files.createDirectories(eventPath.getParent());
        Files.write(eventPath, event.payload());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Saved event to file {}", eventPath);
        }
    }

    //TODO: only delete overrides if migration
    public void deleteAllEvents(String type, Identifier identifier, String format) throws IOException {
        for (String pattern : overridePathPatternsWrite) {
            Path eventPath = getEventFilePath(type, identifier, null, pattern);

            deleteEvent(eventPath);
        }
        Path eventPath = getEventFilePath(type, identifier, null, mainPathPatternWrite);

        deleteEvent(eventPath);

        /*boolean deleted = Files.deleteIfExists(eventPath);
        if (LOGGER.isDebugEnabled() && deleted) {
            LOGGER.debug("Deleted event file {}", eventPath);
        }*/
    }

    private void deleteEvent(Path eventPath) throws IOException {
        if (!Files.isDirectory(eventPath.getParent())) {
            return;
        }

        //TODO: better error handling
        Files.list(eventPath.getParent()).forEach(consumerMayThrow(file -> {
            if (Files.isRegularFile(file) && (Objects.equals(eventPath, file) || file.getFileName().toString().startsWith(eventPath.getFileName().toString() + "."))) {
                String fileName = file.getFileName()
                                      .toString();
                String name = file.toFile()
                                  .getName();
                Path backup;
                if (file.getParent().endsWith("#overrides#")) {
                    backup = file.getParent()
                                 .getParent()
                                 .resolve(".backup/#overrides#");
                } else {
                    backup = file.getParent()
                                 .resolve(".backup");
                }
                Files.createDirectories(backup);
                Files.copy(file, backup.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                Files.delete(file);
                if (LOGGER.isDebugEnabled() ) {
                    LOGGER.debug("Deleted event file {}", eventPath);
                }

                if (file.getParent().endsWith("#overrides#")) {
                    try {
                        Files.delete(file.getParent());
                    } catch (Throwable e) {
                        //ignore
                    }
                }

                boolean stop = true;
            }
        }));
    }

    private Path getEventFilePath(String type, Identifier identifier, String format, String pathPattern) {
        return rootPath.resolve(Paths.get(String.format(pathPattern, type, Joiner.on('/').join(identifier.path()), identifier.id()) + (Objects.nonNull(format) && !format.isEmpty() ? "." + format.toLowerCase() : "")));
    }

    private Stream<MutationEvent> loadEventStream(Pattern pathPattern) {
        if (Objects.isNull(rootPath) || !Files.exists(rootPath)) {
            throw new IllegalArgumentException("Store path does not exist");
        }

        int parentCount = rootPath.getNameCount();

        //TODO: 3 depends on pattern
        try {
            return Files.find(rootPath, 32, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile() && path.getNameCount() - parentCount >= 2)
                        .map(path -> {
                            Matcher matcher = pathPattern.matcher(path.subpath(parentCount, path.getNameCount())
                                                                      .toString());

                            return pathToEvent(path, matcher);
                        })
                        .filter(Objects::nonNull)
                        .sorted()
                        .peek(mutationEvent -> {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Read event {}", mutationEvent);
                            }
                        });
        } catch (IOException e) {
            throw new IllegalStateException("Reading event from store path failed", e);
        }

    }

    private MutationEvent pathToEvent(Path path, Matcher pathMatcher) {
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

            if (Objects.nonNull(eventType) && /*Objects.nonNull(eventPath) &&*/ Objects.nonNull(eventId)) {

                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Reading event from file failed: " + path, e);
                }

                Iterable<String> eventPathSegments = Strings.isNullOrEmpty(eventPath) ? ImmutableList.of() : PATH_SPLITTER.split(eventPath);

                return ImmutableMutationEvent.builder()
                                             .type(eventType)
                                             .identifier(ImmutableIdentifier.builder()
                                                                            .id(eventId)
                                                                            .path(eventPathSegments)
                                                                            .build())
                                             .payload(bytes)
                                             .format(eventPayloadFormat.orElse(null))
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
            //LOGGER.debug("PATH REGEX {} {} {} {} {}", matcher.group(), matcher.groupCount(), matcher.group("name"), matcher.group("separator"), matcher.group("glob"));
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
                    throw new IllegalArgumentException("unknown store path expression: " + matcher.group("glob"));
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

        return Pattern.compile(pattern.toString().replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator()));
    }
}
