package de.ii.xtraplatform.entities.domain

import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class EntityDataStoreStaticsSpec extends Specification {

    // --- entityType ---

    def 'entityType returns the single path element'() {
        given:
        def id = Identifier.from("my-service", "services")

        expect:
        EntityDataStore.entityType(id) == "services"
    }

    def 'entityType returns the last path element for multi-element path'() {
        given:
        def id = Identifier.from("my-service", "group1", "services")

        expect:
        EntityDataStore.entityType(id) == "services"
    }

    def 'entityType throws for empty path'() {
        when:
        EntityDataStore.entityType(Identifier.from("id"))

        then:
        thrown(IllegalArgumentException)
    }

    // --- entityGroup ---

    def 'entityGroup returns empty list for single-element path'() {
        given:
        def id = Identifier.from("my-service", "services")

        expect:
        EntityDataStore.entityGroup(id) == []
    }

    def 'entityGroup returns reversed sublist for multi-element path'() {
        given:
        def id = Identifier.from("my-service", "group1", "group2", "services")

        expect:
        EntityDataStore.entityGroup(id) == ["group2", "group1"]
    }

    def 'entityGroup with two-element path returns single group'() {
        given:
        def id = Identifier.from("my-service", "group1", "services")

        expect:
        EntityDataStore.entityGroup(id) == ["group1"]
    }

    // --- defaults ---

    def 'defaults builds identifier with subType'() {
        given:
        def id = Identifier.from("my-service", "services")
        def result = EntityDataStore.defaults(id, "OGC_API")

        expect:
        result.id() == "defaults"
        result.path().contains("services")
        result.path().contains("ogc_api")
    }

    def 'defaults with Optional subType present delegates to String version'() {
        given:
        def id = Identifier.from("my-service", "services")
        def result = EntityDataStore.defaults(id, Optional.of("OGC_API"))

        expect:
        result.id() == "defaults"
        result.path().contains("services")
        result.path().contains("ogc_api")
    }

    def 'defaults with Optional subType empty omits subType from path'() {
        given:
        def id = Identifier.from("my-service", "services")
        def result = EntityDataStore.defaults(id, Optional.empty())

        expect:
        result.id() == "defaults"
        result.path().contains("services")
        !result.path().any { it == "ogc_api" }
    }

    def 'defaults with group preserves group in path'() {
        given:
        def id = Identifier.from("my-service", "group1", "services")
        def result = EntityDataStore.defaults(id, "OGC_API")

        expect:
        result.id() == "defaults"
        result.path().contains("group1")
        result.path().contains("services")
        result.path().contains("ogc_api")
    }

    // --- COMPARATOR ---

    def 'COMPARATOR sorts by entity type when types differ'() {
        given:
        def providers = Identifier.from("a", "providers")
        def services = Identifier.from("a", "services")

        expect:
        EntityDataStore.COMPARATOR.compare(providers, services) < 0
        EntityDataStore.COMPARATOR.compare(services, providers) > 0
    }

    def 'COMPARATOR returns 0 for same type without tiles suffix'() {
        given:
        def a = Identifier.from("alpha", "services")
        def b = Identifier.from("beta", "services")

        expect:
        EntityDataStore.COMPARATOR.compare(a, b) == 0
    }

    def 'COMPARATOR sorts non-tiles before tiles'() {
        given:
        def regular = Identifier.from("my-service", "services")
        def tiles = Identifier.from("my-service-tiles", "services")

        expect:
        EntityDataStore.COMPARATOR.compare(regular, tiles) == -1
    }

    def 'COMPARATOR sorts tiles after non-tiles'() {
        given:
        def tiles = Identifier.from("my-service-tiles", "services")
        def regular = Identifier.from("my-service", "services")

        expect:
        EntityDataStore.COMPARATOR.compare(tiles, regular) == 1
    }

    def 'COMPARATOR returns 0 for two tiles identifiers'() {
        given:
        def a = Identifier.from("alpha-tiles", "services")
        def b = Identifier.from("beta-tiles", "services")

        expect:
        EntityDataStore.COMPARATOR.compare(a, b) == 0
    }
}
