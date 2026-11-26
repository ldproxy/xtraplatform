package de.ii.xtraplatform.entities.domain

import de.ii.xtraplatform.base.domain.StoreSource
import de.ii.xtraplatform.base.domain.StoreSource.Content
import spock.lang.Specification

import java.nio.file.Path
import java.util.function.Function
import java.util.regex.Pattern

class EventSourceSpec extends Specification {

    // --- pathToEvent with INSTANCES content ---

    def 'standard entity path produces correct event'() {
        given:
        def es = createEventSource(Content.INSTANCES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, path, { "payload".bytes })

        then:
        event != null
        event.type() == "entities"
        event.identifier().id() == "my-service"
        event.identifier().path() == ["services"]
        event.format() == "yml"
    }

    def 'nested path with group produces correct event'() {
        given:
        def es = createEventSource(Content.INSTANCES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("group1/services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, path, { "payload".bytes })

        then:
        event != null
        event.type() == "entities"
        event.identifier().id() == "my-service"
        event.identifier().path() == ["group1", "services"]
    }

    def 'override path produces correct event type'() {
        given:
        def es = createEventSource(Content.OVERRIDES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, path, { "payload".bytes })

        then:
        event != null
        event.type() == "overrides"
        event.identifier().id() == "my-service"
        event.identifier().path() == ["services"]
    }

    def 'defaults path produces correct event type'() {
        given:
        def es = createEventSource(Content.DEFAULTS)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, path, { "payload".bytes })

        then:
        event != null
        event.type() == "defaults"
    }

    def 'non-event content type returns null'() {
        given:
        def es = createEventSource(Content.RESOURCES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.yml")

        expect:
        es.pathToEvent(pattern, path, { "payload".bytes }) == null
    }

    def 'path not matching pattern returns null'() {
        given:
        def es = createEventSource(Content.INSTANCES)
        def pattern = es.pathPatternStream.findFirst().get()
        // Path that doesn't match - no entity type directory
        def path = storePath(".hidden")

        expect:
        es.pathToEvent(pattern, path, { "payload".bytes }) == null
    }

    def 'json format is extracted correctly'() {
        given:
        def es = createEventSource(Content.INSTANCES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.json")

        when:
        def event = es.pathToEvent(pattern, path, { "payload".bytes })

        then:
        event != null
        event.format() == "json"
    }

    def 'payload supplier is invoked'() {
        given:
        def es = createEventSource(Content.INSTANCES)
        def pattern = es.pathPatternStream.findFirst().get()
        def path = storePath("services/my-service.yml")
        def payloadBytes = '{"key":"value"}'.bytes

        when:
        def event = es.pathToEvent(pattern, path, { payloadBytes })

        then:
        event.payload() == payloadBytes
    }

    // --- getSavePath round-trip ---

    def 'getSavePath round-trips with pathToEvent for ENTITIES content'() {
        given:
        def es = createEventSource(Content.ENTITIES)
        def pattern = es.pathPatternStream.findFirst().get()
        def originalPath = storePath("entities/services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, originalPath, { "payload".bytes })
        def savePath = es.getSavePath(event)

        then:
        event != null
        savePath == originalPath
    }

    def 'getSavePath round-trips with nested path'() {
        given:
        def es = createEventSource(Content.ENTITIES)
        def pattern = es.pathPatternStream.findFirst().get()
        def originalPath = storePath("entities/group1/services/my-service.yml")

        when:
        def event = es.pathToEvent(pattern, originalPath, { "payload".bytes })
        def savePath = es.getSavePath(event)

        then:
        event != null
        savePath == originalPath
    }

    // --- helpers ---

    // --- applyPrefixes for watched paths ---

    def 'watched path is mapped to the canonical store layout'() {
        given:
        def es = createEventSource(content)

        expect:
        es.applyPrefixes(Path.of(watched)) == Path.of(canonical)

        where:
        content           | watched                          || canonical
        Content.ALL       | "instances/grp/services/a.yml"   || "entities/grp/services/a.yml"
        Content.ALL       | "overrides/services/_type.yml"   || "overrides/services/_type.yml"
        Content.INSTANCES | "grp/services/a.yml"             || "entities/grp/services/a.yml"
        Content.OVERRIDES | "services/a.yml"                 || "overrides/services/a.yml"
    }

    private static final Path ROOT = Path.of("/store")

    private Path storePath(String relativePath) {
        ROOT.resolve(relativePath)
    }

    private EventSource createEventSource(Content content) {
        def source = Stub(StoreSource) {
            getContent() >> content
            isArchive() >> false
            getPrefix() >> Optional.empty()
            getLabel() >> "test"
            getIncludes() >> []
            getExcludes() >> []
            isSingleContent() >> (content != Content.ALL && content != Content.MULTI && content != Content.ENTITIES)
        }
        new EventSource(ROOT, source, Function.identity())
    }
}
