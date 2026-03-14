package de.ii.xtraplatform.entities.domain

import dagger.Lazy
import spock.lang.Specification

class EntityFactoriesImplSpec extends Specification {

    def 'get by type finds factory case-insensitively'() {
        given:
        def factory = stubFactory("services", null)
        def factories = createFactories(factory)

        expect:
        factories.get("services") == factory
        factories.get("SERVICES") == factory
        factories.get("Services") == factory
    }

    def 'get by type throws NoSuchElementException when not found'() {
        given:
        def factories = createFactories(stubFactory("services", null))

        when:
        factories.get("providers")

        then:
        thrown(NoSuchElementException)
    }

    def 'get by type and subType finds matching factory'() {
        given:
        def factory = stubFactory("services", "OGC_API")
        def factories = createFactories(factory)

        expect:
        factories.get("services", "OGC_API") == factory
        factories.get("SERVICES", "ogc_api") == factory
    }

    def 'get by type and subType throws when not found'() {
        given:
        def factories = createFactories(stubFactory("services", null))

        when:
        factories.get("services", "OGC_API")

        then:
        thrown(NoSuchElementException)
    }

    def 'get with Optional subType present delegates to get(type, subType)'() {
        given:
        def subFactory = stubFactory("services", "OGC_API")
        def factories = createFactories(subFactory)

        expect:
        factories.get("services", Optional.of("OGC_API")) == subFactory
    }

    def 'get with Optional subType empty delegates to get(type)'() {
        given:
        def baseFactory = stubFactory("services", null)
        def factories = createFactories(baseFactory)

        expect:
        factories.get("services", Optional.empty()) == baseFactory
    }

    def 'getAll by type returns all matching factories'() {
        given:
        def f1 = stubFactory("services", null)
        def f2 = stubFactory("services", "OGC_API")
        def f3 = stubFactory("providers", null)
        def factories = createFactories(f1, f2, f3)

        when:
        def result = factories.getAll("services")

        then:
        result.size() == 2
        result.containsAll([f1, f2])
    }

    def 'getAll returns empty set when no match'() {
        given:
        def factories = createFactories(stubFactory("services", null))

        expect:
        factories.getAll("providers").isEmpty()
    }

    def 'getTypes returns set of all type strings'() {
        given:
        def factories = createFactories(
                stubFactory("services", null),
                stubFactory("services", "OGC_API"),
                stubFactory("providers", null)
        )

        expect:
        factories.getTypes() == ["services", "providers"] as Set
    }

    def 'getSubTypes returns subtypes excluding exact type match'() {
        given:
        def base = stubFactory("services", null)
        def sub1 = stubFactory("services", "OGC_API")
        def sub2 = stubFactory("services", "TILES")
        def factories = createFactories(base, sub1, sub2)

        when:
        def result = factories.getSubTypes("services", [])

        then:
        result.size() == 2
        result.containsAll(["ogc_api", "tiles"])
    }

    def 'getSubTypes with existing subType returns deeper subtypes'() {
        given:
        def base = stubFactory("services", null)
        def sub = stubFactory("services", "OGC_API")
        // fullType must be lowercase to match getSpecificEntityType which lowercases
        def deepSub = stubFactoryWithFullType("services", "OGC_API/FEATURES", "services/ogc_api/features")
        def factories = createFactories(base, sub, deepSub)

        when:
        def result = factories.getSubTypes("services", ["OGC_API"])

        then:
        result.size() == 1
        result.contains("ogc_api/features")
    }

    // --- helpers ---

    private EntityFactoriesImpl createFactories(EntityFactory... factories) {
        def factorySet = new HashSet<EntityFactory>(Arrays.asList(factories))
        new EntityFactoriesImpl(new Lazy<Set<EntityFactory>>() {
            Set<EntityFactory> get() { return factorySet }
        })
    }

    private EntityFactory stubFactory(String type, String subType) {
        stubFactoryWithFullType(type, subType, subType != null ? "${type}/${subType}" : type)
    }

    private EntityFactory stubFactoryWithFullType(String type, String subType, String fullType) {
        def factory = Stub(EntityFactory)
        factory.type() >> type
        factory.subType() >> (subType != null ? Optional.of(subType) : Optional.empty())
        factory.fullType() >> fullType
        return factory
    }
}
