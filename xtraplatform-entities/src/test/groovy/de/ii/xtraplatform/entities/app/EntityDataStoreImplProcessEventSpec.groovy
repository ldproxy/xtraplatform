package de.ii.xtraplatform.entities.app

import de.ii.xtraplatform.entities.domain.EntityDataStore
import de.ii.xtraplatform.entities.domain.EntityFactory
import de.ii.xtraplatform.entities.domain.EntityFactoriesImpl
import de.ii.xtraplatform.entities.domain.ImmutableReplayEvent
import de.ii.xtraplatform.entities.domain.ReplayEvent
import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.function.Predicate

class EntityDataStoreImplProcessEventSpec extends Specification {

    EntityDataStoreImpl store
    EventSourcing eventSourcing
    ValueEncodingJacksonWithNesting valueEncoding
    EntityFactoriesImpl entityFactories

    def setup() {
        eventSourcing = Stub(EventSourcing)
        valueEncoding = Stub(ValueEncodingJacksonWithNesting)
        entityFactories = Stub(EntityFactoriesImpl)

        // Defaults
        valueEncoding.isEmpty(_) >> false
        valueEncoding.isSupported(_) >> true
        entityFactories.getTypes() >> Set.of("services", "providers")

        store = createMinimalStore()
        setField(store, "valueEncoding", valueEncoding)
        setField(store, "eventSourcing", eventSourcing)
        setField(store, "entityFactories", entityFactories)
    }

    // --- Entity event tests ---

    def 'entity event with no duplicate returns event as-is'() {
        given:
        def event = entityEvent("my-service", "services")
        eventSourcing.has(_ as Predicate) >> false
        eventSourcing.has(_ as Identifier) >> false

        expect:
        callProcessEvent(event) == [event]
    }

    def 'entity event with duplicate id in different group returns empty'() {
        given:
        def event = entityEvent("my-service", "group1", "services")
        eventSourcing.has(_ as Predicate) >> true

        expect:
        callProcessEvent(event).isEmpty()
    }

    def 'entity event that already exists returns empty'() {
        given:
        def event = entityEvent("my-service", "services")
        eventSourcing.has(_ as Predicate) >> false
        eventSourcing.has(_ as Identifier) >> true

        expect:
        callProcessEvent(event).isEmpty()
    }

    def 'delete entity event returns event as-is, skips duplicate check'() {
        given:
        def event = ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from("my-service", "services"))
                .format("yml")
                .payload("id: my-service".getBytes(StandardCharsets.UTF_8))
                .deleted(true)
                .build()

        expect:
        callProcessEvent(event) == [event]
    }

    // --- Override event tests ---

    def 'override event with matching entity returns event with remapped identifier'() {
        given:
        def event = overrideEvent("my-service", "services")
        def cacheKey = Identifier.from("my-service", "services")
        eventSourcing.has(_ as Identifier) >> { Identifier id ->
            id == cacheKey
        }

        when:
        def result = callProcessEvent(event)

        then:
        result.size() == 1
        result[0].identifier() == cacheKey
    }

    def 'override event with no matching entity returns empty'() {
        given:
        def event = overrideEvent("my-service", "services")
        eventSourcing.has(_ as Identifier) >> false

        expect:
        callProcessEvent(event).isEmpty()
    }

    def 'event with empty payload returns empty'() {
        given:
        // isEmpty is final, so use actually empty content that matches the YAML_EMPTY or JSON_EMPTY patterns
        def event = ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from("my-service", "services"))
                .format("yml")
                .payload("   ".getBytes(StandardCharsets.UTF_8))
                .build()

        expect:
        callProcessEvent(event).isEmpty()
    }

    def 'event with unsupported format returns empty'() {
        given:
        def unsupportedEncoding = Stub(ValueEncodingJacksonWithNesting) {
            isEmpty(_) >> false
            isSupported(_) >> false
        }
        store = createMinimalStore()
        setField(store, "valueEncoding", unsupportedEncoding)
        setField(store, "eventSourcing", eventSourcing)
        setField(store, "entityFactories", entityFactories)

        def event = entityEvent("my-service", "services")

        expect:
        callProcessEvent(event).isEmpty()
    }

    // --- Type override tests ---

    def 'type override with matching entities returns one event per entity'() {
        given:
        def event = overrideEvent("_type", "services")
        def entity1 = Identifier.from("svc1", "services")
        def entity2 = Identifier.from("svc2", "services")
        eventSourcing.getIdentifiers("services") >> [entity1, entity2]

        when:
        def result = callProcessEvent(event)

        then:
        result.size() == 2
        result.collect { it.identifier() } as Set == [entity1, entity2] as Set
    }

    def 'type override with no matching entities returns empty'() {
        given:
        def event = overrideEvent("_type", "services")
        eventSourcing.getIdentifiers("services") >> []

        expect:
        callProcessEvent(event).isEmpty()
    }

    def 'key-path type override returns events with nested payload per matching entity'() {
        given:
        def event = ImmutableReplayEvent.builder()
                .type("overrides")
                .identifier(Identifier.from("apiDoc", "services", "_type"))
                .format("yml")
                .payload("enabled: true".getBytes(StandardCharsets.UTF_8))
                .build()
        def entity1 = Identifier.from("svc1", "services")
        eventSourcing.getIdentifiers("services") >> [entity1]

        def nestedPayload = "apiDoc:\n  enabled: true".getBytes(StandardCharsets.UTF_8)
        def factory = Stub(EntityFactory)
        factory.getKeyPathAlias(_) >> Optional.empty()
        entityFactories.get("services") >> factory
        valueEncoding.nestPayload(_, _, _, _) >> nestedPayload

        when:
        def result = callProcessEvent(event)

        then:
        result.size() == 1
        result[0].identifier() == entity1
        result[0].payload() == nestedPayload
    }

    // --- Helpers ---

    private List<ReplayEvent> callProcessEvent(ReplayEvent event) {
        def method = EntityDataStoreImpl.getDeclaredMethod("processEvent", ReplayEvent)
        method.setAccessible(true)
        return (List<ReplayEvent>) method.invoke(store, event)
    }

    private static ReplayEvent entityEvent(String id, String... path) {
        ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from(id, path))
                .format("yml")
                .payload("id: ${id}".getBytes(StandardCharsets.UTF_8))
                .build()
    }

    private static ReplayEvent overrideEvent(String id, String... path) {
        ImmutableReplayEvent.builder()
                .type("overrides")
                .identifier(Identifier.from(id, path))
                .format("yml")
                .payload("enabled: true".getBytes(StandardCharsets.UTF_8))
                .build()
    }

    private static EntityDataStoreImpl createMinimalStore() {
        def unsafeClass = Class.forName("sun.misc.Unsafe")
        def unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.setAccessible(true)
        def unsafe = unsafeField.get(null)
        return (EntityDataStoreImpl) unsafe.allocateInstance(EntityDataStoreImpl)
    }

    private static void setField(Object obj, String fieldName, Object value) {
        def field = obj.getClass().getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(obj, value)
    }
}
