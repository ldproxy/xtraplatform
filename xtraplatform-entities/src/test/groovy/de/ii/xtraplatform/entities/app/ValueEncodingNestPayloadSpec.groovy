package de.ii.xtraplatform.entities.app

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.ii.xtraplatform.base.domain.Jackson
import de.ii.xtraplatform.entities.domain.KeyPathAlias
import io.dropwizard.util.DataSize
import spock.lang.Specification

class ValueEncodingNestPayloadSpec extends Specification {

    ValueEncodingJacksonWithNesting encoding

    def setup() {
        def jackson = Stub(Jackson) {
            getDefaultObjectMapper() >> new ObjectMapper()
            getNewObjectMapper(_) >> { args -> new ObjectMapper((JsonFactory) args[0]) }
        }
        encoding = new ValueEncodingJacksonWithNesting(jackson, DataSize.megabytes(3), false)
    }

    def 'empty key path returns payload unchanged'() {
        given:
        def payload = '{"a":"value"}'.bytes

        expect:
        encoding.nestPayload(payload, "json", [], Optional.empty()) == payload
    }

    def 'single-element key path wraps payload under that key'() {
        given:
        def payload = '{"inner":"value"}'.bytes

        when:
        def result = encoding.nestPayload(payload, "json", ["outer"], Optional.empty())

        then:
        parseJson(result) == [outer: [inner: "value"]]
    }

    def 'multi-element key path wraps payload under nested keys'() {
        given:
        def payload = '{"leaf":"value"}'.bytes

        when:
        def result = encoding.nestPayload(payload, "json", ["l1", "l2"], Optional.empty())

        then:
        parseJson(result) == [l1: [l2: [leaf: "value"]]]
    }

    def 'three-level key path nests correctly'() {
        given:
        def payload = '{"x":1}'.bytes

        when:
        def result = encoding.nestPayload(payload, "json", ["a", "b", "c"], Optional.empty())

        then:
        parseJson(result) == [a: [b: [c: [x: 1]]]]
    }

    def 'with KeyPathAlias the alias wrapping is applied at innermost level'() {
        given:
        def payload = '{"field":"value"}'.bytes
        def alias = Stub(KeyPathAlias) {
            wrapMap(_) >> { args -> [aliased: args[0]] }
        }

        when:
        def result = encoding.nestPayload(payload, "json", ["outer", "inner"], Optional.of(alias))

        then:
        // inner (last element) uses alias, outer wraps normally
        parseJson(result) == [outer: [aliased: [field: "value"]]]
    }

    def 'with KeyPathAlias and single-element path only alias is applied'() {
        given:
        def payload = '{"field":"value"}'.bytes
        def alias = Stub(KeyPathAlias) {
            wrapMap(_) >> { args -> [wrapped: args[0]] }
        }

        when:
        def result = encoding.nestPayload(payload, "json", ["key"], Optional.of(alias))

        then:
        parseJson(result) == [wrapped: [field: "value"]]
    }

    def 'yaml format handling'() {
        given:
        def payload = '---\nname: test\n'.bytes

        when:
        def result = encoding.nestPayload(payload, "yml", ["outer"], Optional.empty())

        then:
        def parsed = parseYaml(result)
        parsed == [outer: [name: "test"]]
    }

    def 'unsupported format throws NullPointerException'() {
        given:
        def payload = '{"a":1}'.bytes

        when:
        encoding.nestPayload(payload, "unknown_format", ["key"], Optional.empty())

        then:
        // FORMAT.fromString returns UNKNOWN, getMapper(UNKNOWN) returns null → NPE
        thrown(NullPointerException)
    }

    private Map<String, Object> parseJson(byte[] bytes) {
        new ObjectMapper().readValue(bytes, new TypeReference<LinkedHashMap<String, Object>>() {})
    }

    private Map<String, Object> parseYaml(byte[] bytes) {
        // The encoding's YAML mapper produces YAML output; parse it back
        new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
                .readValue(bytes, new TypeReference<LinkedHashMap<String, Object>>() {})
    }
}
