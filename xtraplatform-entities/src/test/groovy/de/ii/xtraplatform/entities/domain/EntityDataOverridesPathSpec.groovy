package de.ii.xtraplatform.entities.domain

import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class EntityDataOverridesPathSpec extends Specification {

    static final Set<String> ENTITY_TYPES = Set.of("services", "providers")

    def 'simple override: path=["services"], id="my-service"'() {
        given:
        def identifier = Identifier.from("my-service", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getEntityType() == "services"
        path.getEntityId() == "my-service"
        path.getKeyPath() == []
        path.getGroups() == []
    }

    def 'key-path override: path=["services", "my-service"], id="apiDocumentation"'() {
        given:
        def identifier = Identifier.from("apiDocumentation", "services", "my-service")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getEntityType() == "services"
        path.getEntityId() == "my-service"
        path.getKeyPath() == ["apiDocumentation"]
    }

    def 'override with groups: path=["group1", "services"], id="my-service"'() {
        given:
        def identifier = Identifier.from("my-service", "group1", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getGroups() == ["group1"]
        path.getEntityType() == "services"
        path.getEntityId() == "my-service"
        path.getKeyPath() == []
    }

    def 'asIdentifier reconstructs correct identifier'() {
        given:
        def identifier = Identifier.from("my-service", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)
        def reconstructed = path.asIdentifier()

        then:
        reconstructed.id() == "my-service"
        reconstructed.path() == ["services"]
    }

    def 'asIdentifier with groups reconstructs correctly'() {
        given:
        def identifier = Identifier.from("my-service", "group1", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)
        def reconstructed = path.asIdentifier()

        then:
        reconstructed.id() == "my-service"
        reconstructed.path() == ["group1", "services"]
    }

    def 'invalid path with less than 2 segments throws IllegalArgumentException'() {
        given:
        def identifier = Identifier.from("lonely")

        when:
        EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        thrown(IllegalArgumentException)
    }

    def 'type override: path=["services"], id="_type"'() {
        given:
        def identifier = Identifier.from("_type", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getEntityType() == "services"
        path.getEntityId() == "_type"
        path.isTypeOverride()
    }

    def 'key-path type override: path=["services", "_type"], id="apiDocumentation"'() {
        given:
        def identifier = Identifier.from("apiDocumentation", "services", "_type")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getEntityType() == "services"
        path.getEntityId() == "_type"
        path.getKeyPath() == ["apiDocumentation"]
        path.isTypeOverride()
    }

    def 'key-path type override with groups: path=["group1", "services", "_type"], id="apiDocumentation"'() {
        given:
        def identifier = Identifier.from("apiDocumentation", "group1", "services", "_type")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        path.getGroups() == ["group1"]
        path.getEntityType() == "services"
        path.getEntityId() == "_type"
        path.getKeyPath() == ["apiDocumentation"]
        path.isTypeOverride()
    }

    def 'normal override is not type override'() {
        given:
        def identifier = Identifier.from("my-service", "services")

        when:
        def path = EntityDataOverridesPath.from(identifier, ENTITY_TYPES)

        then:
        !path.isTypeOverride()
    }
}
