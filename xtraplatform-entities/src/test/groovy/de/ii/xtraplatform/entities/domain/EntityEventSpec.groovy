package de.ii.xtraplatform.entities.domain

import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class EntityEventSpec extends Specification {

    def 'events with different type sort by type string'() {
        given:
        def defaults = event("defaults", Identifier.from("a", "services"))
        def entities = event("entities", Identifier.from("a", "services"))
        def overrides = event("overrides", Identifier.from("a", "services"))

        expect:
        defaults.compareTo(entities) < 0
        entities.compareTo(overrides) < 0
        defaults.compareTo(overrides) < 0
    }

    def 'events with same type sort by identifier'() {
        given:
        def a = event("entities", Identifier.from("aaa", "services"))
        def b = event("entities", Identifier.from("bbb", "services"))

        expect:
        a.compareTo(b) < 0
        b.compareTo(a) > 0
        a.compareTo(a) == 0
    }

    def 'type override with _type as id sorts before entity-specific override'() {
        given:
        def typeOverride = event("overrides", Identifier.from("_type", "services"))
        def entityOverride = event("overrides", Identifier.from("my-service", "services"))

        expect:
        typeOverride.compareTo(entityOverride) < 0
        entityOverride.compareTo(typeOverride) > 0
    }

    def 'type override with _type in path sorts before entity-specific override'() {
        given:
        def keyPathTypeOverride = event("overrides", Identifier.from("apiDoc", "services", "_type"))
        def entityOverride = event("overrides", Identifier.from("my-service", "services"))

        expect:
        keyPathTypeOverride.compareTo(entityOverride) < 0
        entityOverride.compareTo(keyPathTypeOverride) > 0
    }

    def 'two type overrides sort by identifier'() {
        given:
        def a = event("overrides", Identifier.from("_type", "providers"))
        def b = event("overrides", Identifier.from("_type", "services"))

        expect:
        a.compareTo(b) < 0
        b.compareTo(a) > 0
    }

    def 'two non-type-override events sort by identifier'() {
        given:
        def a = event("overrides", Identifier.from("alpha", "services"))
        def b = event("overrides", Identifier.from("beta", "services"))

        expect:
        a.compareTo(b) < 0
        b.compareTo(a) > 0
    }

    def 'isDelete returns true when deleted is true'() {
        given:
        def deleted = ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from("a", "services"))
                .format("yml")
                .deleted(true)
                .build()

        expect:
        deleted.isDelete()
    }

    def 'isDelete returns false when deleted is false or null'() {
        given:
        def notDeleted = event("entities", Identifier.from("a", "services"))
        def explicitFalse = ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from("a", "services"))
                .format("yml")
                .deleted(false)
                .build()

        expect:
        !notDeleted.isDelete()
        !explicitFalse.isDelete()
    }

    def 'asPath formats correctly'() {
        given:
        def e = event("entities", Identifier.from("my-service", "services"))

        expect:
        e.asPath() == "entities/services/my-service.yml"
    }

    def 'asPathNoType formats correctly'() {
        given:
        def e = event("overrides", Identifier.from("my-service", "services"))

        expect:
        e.asPathNoType() == "services/my-service.yml"
    }

    def 'asPath with nested identifier'() {
        given:
        def e = event("entities", Identifier.from("my-service", "group1", "services"))

        expect:
        e.asPath() == "entities/group1/services/my-service.yml"
    }

    private static ReplayEvent event(String type, de.ii.xtraplatform.values.domain.Identifier identifier) {
        return ImmutableReplayEvent.builder()
                .type(type)
                .identifier(identifier)
                .format("yml")
                .build()
    }
}
