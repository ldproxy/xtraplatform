package de.ii.xtraplatform.entities.app

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import de.ii.xtraplatform.base.domain.Jackson
import de.ii.xtraplatform.entities.domain.EntityFactory
import de.ii.xtraplatform.entities.domain.EntityFactoriesImpl
import de.ii.xtraplatform.entities.domain.ImmutableEventFilter
import de.ii.xtraplatform.entities.domain.ImmutableReplayEvent
import de.ii.xtraplatform.entities.domain.ReplayEvent
import de.ii.xtraplatform.values.domain.Identifier
import io.dropwizard.util.DataSize
import org.objenesis.ObjenesisStd
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
        valueEncoding = realValueEncoding()
        entityFactories = Stub(EntityFactoriesImpl)

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
        def event = ImmutableReplayEvent.builder()
                .type("entities")
                .identifier(Identifier.from("my-service", "services"))
                .format("xml")
                .payload("<id>my-service</id>".getBytes(StandardCharsets.UTF_8))
                .build()

        expect:
        callProcessEvent(event).isEmpty()
    }

    // --- Type override tests ---

    def 'type override with matching entities returns one event per entity'() {
        given:
        def event = overrideEvent("_type", "services")
        def entity1 = Identifier.from("svc1", "services")
        def entity2 = Identifier.from("svc2", "services")
        eventSourcing.getIdentifiers() >> [entity1, entity2]

        when:
        def result = callProcessEvent(event)

        then:
        result.size() == 2
        result.collect { it.identifier() } as Set == [entity1, entity2] as Set
    }

    def 'type override without groups applies to entities of the type in every group'() {
        given:
        def event = overrideEvent("_type", "services")
        def ungrouped = Identifier.from("svc1", "services")
        def grouped = Identifier.from("svc2", "group1", "services")
        def otherType = Identifier.from("prov1", "providers")
        eventSourcing.getIdentifiers() >> [ungrouped, grouped, otherType]

        expect:
        callProcessEvent(event).collect { it.identifier() } as Set == [ungrouped, grouped] as Set
    }

    def 'type override with groups applies to entities in that group and its child groups'() {
        given:
        def event = overrideEvent("_type", "group1", "services")
        def ungrouped = Identifier.from("svc1", "services")
        def grouped = Identifier.from("svc2", "group1", "services")
        def childGroup = Identifier.from("svc3", "group1", "child", "services")
        def otherGroup = Identifier.from("svc4", "group2", "services")
        def otherWithChild = Identifier.from("svc5", "group2", "group1", "services")
        eventSourcing.getIdentifiers() >> [ungrouped, grouped, childGroup, otherGroup, otherWithChild]

        expect:
        callProcessEvent(event).collect { it.identifier() } as Set == [grouped, childGroup] as Set
    }

    def 'type override with entity filter applies only to the filtered entities'() {
        given:
        def filter = ImmutableEventFilter.builder()
                .addEntityTypes("services")
                .addIds("svc2")
                .build()
        def event = ImmutableReplayEvent.copyOf(overrideEvent("_type", "services"))
                .withEntityFilter(filter)
        def entity1 = Identifier.from("svc1", "services")
        def entity2 = Identifier.from("svc2", "services")
        eventSourcing.getIdentifiers() >> [entity1, entity2]

        expect:
        callProcessEvent(event).collect { it.identifier() } == [entity2]
    }

    def 'type override with no matching entities returns empty'() {
        given:
        def event = overrideEvent("_type", "services")
        eventSourcing.getIdentifiers() >> []

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
        eventSourcing.getIdentifiers() >> [entity1]

        def factory = Stub(EntityFactory)
        factory.getKeyPathAlias(_) >> Optional.empty()
        entityFactories.get("services") >> factory

        when:
        def result = callProcessEvent(event)

        then:
        result.size() == 1
        result[0].identifier() == entity1
        new String(result[0].payload(), StandardCharsets.UTF_8).trim() == "---\napiDoc:\n  enabled: true"
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

    private ValueEncodingJacksonWithNesting realValueEncoding() {
        def jackson = Stub(Jackson) {
            getDefaultObjectMapper() >> new ObjectMapper()
            getNewObjectMapper(_) >> { args -> new ObjectMapper((JsonFactory) args[0]) }
        }
        return new ValueEncodingJacksonWithNesting(jackson, DataSize.megabytes(3), false)
    }

    private static EntityDataStoreImpl createMinimalStore() {
        return new ObjenesisStd().newInstance(EntityDataStoreImpl)
    }

    private static void setField(Object obj, String fieldName, Object value) {
        def field = obj.getClass().getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(obj, value)
    }
}
