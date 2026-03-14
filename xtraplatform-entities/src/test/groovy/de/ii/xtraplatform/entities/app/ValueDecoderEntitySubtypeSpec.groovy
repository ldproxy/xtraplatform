package de.ii.xtraplatform.entities.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import de.ii.xtraplatform.entities.domain.EntityData
import de.ii.xtraplatform.entities.domain.EntityDataBuilder
import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class ValueDecoderEntitySubtypeSpec extends Specification {

    def identifier = Identifier.from("my-service", "services")
    def payload = '{"id":"my-service"}'.bytes

    def 'data without entitySubType returns data unchanged'() {
        given:
        def data = Stub(EntityData) {
            getEntitySubType() >> Optional.empty()
        }
        def decoder = new ValueDecoderEntitySubtype({ id, st -> null }, Stub(EventSourcing))

        expect:
        decoder.process(identifier, payload, new ObjectMapper(), data, false) == data
    }

    def 'data with entitySubType and null builder returns original data'() {
        given:
        def data = Stub(EntityData) {
            getEntitySubType() >> Optional.of("OGC_API")
        }
        def decoder = new ValueDecoderEntitySubtype({ id, st -> null }, Stub(EventSourcing))

        expect:
        decoder.process(identifier, payload, new ObjectMapper(), data, false) == data
    }

    def 'data with entitySubType and builder returns rebuilt data'() {
        given:
        def builtData = Stub(EntityData)
        def builder = Mock(EntityDataBuilder) {
            build() >> builtData
        }
        def data = Stub(EntityData) {
            getEntitySubType() >> Optional.of("OGC_API")
        }
        def eventSourcing = Stub(EventSourcing) {
            has(identifier) >> false
        }
        def objectMapper = Mock(ObjectMapper) {
            readerForUpdating(builder) >> Stub(ObjectReader) {
                readValue(payload) >> builder
            }
        }
        def decoder = new ValueDecoderEntitySubtype({ id, st -> builder }, eventSourcing)

        expect:
        decoder.process(identifier, payload, objectMapper, data, false) == builtData
    }

    def 'cache hit copies cached entity into builder'() {
        given:
        def cachedData = Stub(EntityData)
        def builtData = Stub(EntityData)
        def builder = Mock(EntityDataBuilder) {
            build() >> builtData
        }
        def data = Stub(EntityData) {
            getEntitySubType() >> Optional.of("OGC_API")
        }
        def eventSourcing = Stub(EventSourcing) {
            has(identifier) >> true
            get(identifier) >> cachedData
        }
        def objectMapper = Mock(ObjectMapper) {
            readerForUpdating(builder) >> Stub(ObjectReader) {
                readValue(payload) >> builder
            }
        }
        def decoder = new ValueDecoderEntitySubtype({ id, st -> builder }, eventSourcing)

        when:
        def result = decoder.process(identifier, payload, objectMapper, data, false)

        then:
        1 * builder.from(cachedData)
        result == builtData
    }

    def 'ignoreCache flag skips cache copy'() {
        given:
        def cachedData = Stub(EntityData)
        def builtData = Stub(EntityData)
        def builder = Mock(EntityDataBuilder) {
            build() >> builtData
        }
        def data = Stub(EntityData) {
            getEntitySubType() >> Optional.of("OGC_API")
        }
        def eventSourcing = Stub(EventSourcing) {
            has(identifier) >> true
            get(identifier) >> cachedData
        }
        def objectMapper = Mock(ObjectMapper) {
            readerForUpdating(builder) >> Stub(ObjectReader) {
                readValue(payload) >> builder
            }
        }
        def decoder = new ValueDecoderEntitySubtype({ id, st -> builder }, eventSourcing)

        when:
        def result = decoder.process(identifier, payload, objectMapper, data, true)

        then:
        0 * builder.from(_)
        result == builtData
    }
}
