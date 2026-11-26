package de.ii.xtraplatform.entities.domain

import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Path

class EventFilterSpec extends Specification {

    def 'filter for a single entity also matches type overrides of its type'() {
        given:
        def filter = EventFilter.fromPath(Path.of("entities/services/svc1.yml"))

        expect:
        filter.matches(override(Identifier.from(id, path as String[]))) == expected

        where:
        id                     | path                    || expected
        "_type"                | ["services"]            || true
        "_type.api"            | ["services"]            || true
        "api"                  | ["services", "_type"]   || true
        "svc1"                 | ["services"]            || true
        "svc2"                 | ["services"]            || false
        "_type"                | ["providers"]           || false
    }

    def 'changed type override reloads every entity of the type'() {
        given:
        def filter = EventFilter.fromPath(Path.of(changed))

        expect:
        !filter.isRestrictedToIds()
        filter.matches(entity(Identifier.from("svc1", "services")))
        filter.matches(entity(Identifier.from("svc2", "group1", "services")))

        where:
        changed << [
                "overrides/services/_type.yml",
                "overrides/services/_type.api.yml",
                "overrides/services/_type/api.yml"
        ]
    }

    def 'changed entity override reloads only that entity'() {
        given:
        def filter = EventFilter.fromPath(Path.of("overrides/services/svc1.yml"))

        expect:
        filter.isRestrictedToIds()
        filter.matches(entity(Identifier.from("svc1", "services")))
        !filter.matches(entity(Identifier.from("svc2", "services")))
    }

    private static EntityEvent override(Identifier identifier) {
        event("overrides", identifier)
    }

    private static EntityEvent entity(Identifier identifier) {
        event("entities", identifier)
    }

    private static EntityEvent event(String type, Identifier identifier) {
        ImmutableReplayEvent.builder()
                .type(type)
                .identifier(identifier)
                .format("yml")
                .payload("a: b".getBytes(StandardCharsets.UTF_8))
                .build()
    }
}
