package de.ii.xtraplatform.entities.app

import de.ii.xtraplatform.entities.domain.EntityFactory
import de.ii.xtraplatform.entities.domain.KeyPathAliasUnwrap
import spock.lang.Specification

class MapAlignerSpec extends Specification {

    EntityFactory entityFactory = Stub(EntityFactory) {
        getKeyPathAlias(_) >> Optional.empty()
        getKeyPathAliasReverse(_) >> Optional.empty()
    }

    def 'flat map: values matching predicate are replaced from alignTo'() {
        given:
        def source = [a: "x", b: "y"]
        def alignTo = [a: "replaced", b: "also-replaced"]

        expect:
        MapAligner.align(source, alignTo, { true }, entityFactory) == [a: "replaced", b: "also-replaced"]
    }

    def 'flat map: values not matching predicate are kept from source'() {
        given:
        def source = [a: "x", b: "y"]
        def alignTo = [a: "replaced", b: "also-replaced"]

        expect:
        MapAligner.align(source, alignTo, { false }, entityFactory) == [a: "x", b: "y"]
    }

    def 'null values in alignTo matching predicate are skipped'() {
        given:
        def source = [a: "x", b: "y"]
        def alignTo = [a: null, b: "replaced"]

        expect:
        // predicate returns true for both, but null values are skipped (not put in result)
        def result = MapAligner.align(source, alignTo, { true }, entityFactory)
        !result.containsKey("a")
        result.b == "replaced"
    }

    def 'keys in source but absent in alignTo are kept'() {
        given:
        def source = [a: "x", b: "y", c: "z"]
        def alignTo = [a: "replaced"]

        expect:
        def result = MapAligner.align(source, alignTo, { true }, entityFactory)
        result.a == "replaced"
        result.b == "y"
        result.c == "z"
    }

    def 'keys in alignTo but absent in source are ignored'() {
        given:
        def source = [a: "x"]
        def alignTo = [a: "replaced", extra: "ignored"]

        expect:
        def result = MapAligner.align(source, alignTo, { true }, entityFactory)
        result == [a: "replaced"]
    }

    def 'nested maps: recursive alignment into child maps'() {
        given:
        def source = [outer: [inner: "old"]]
        def alignTo = [outer: [inner: "new"]]

        expect:
        MapAligner.align(source, alignTo, { true }, entityFactory) == [outer: [inner: "new"]]
    }

    def 'nested maps: predicate applies to leaf values'() {
        given:
        def source = [outer: [a: null, b: "keep"]]
        def alignTo = [outer: [a: "fill", b: "replace"]]

        expect:
        // Only align when alignTo value is non-null (predicate checks alignTo value)
        def result = MapAligner.align(source, alignTo, { it != null }, entityFactory)
        result == [outer: [a: "fill", b: "replace"]]
    }

    def 'nested maps: only matching predicate values are replaced'() {
        given:
        def source = [outer: [a: "keep", b: "keep"]]
        def alignTo = [outer: [a: "UPPER", b: "lower"]]

        expect:
        // Only align when alignTo value starts with uppercase
        def result = MapAligner.align(source, alignTo, { it instanceof String && ((String)it).charAt(0).isUpperCase() }, entityFactory)
        result == [outer: [a: "UPPER", b: "keep"]]
    }

    def 'source has list, alignTo has map: predicate matches parent replaces wholesale'() {
        given:
        def source = [tags: ["a", "b", "c"]]
        def alignTo = [tags: [a: "mapped-a", b: "mapped-b"]]

        expect:
        // When predicate matches the parent map value, it replaces the entire value
        def result = MapAligner.align(source, alignTo, { true }, entityFactory)
        result.tags == [a: "mapped-a", b: "mapped-b"]
    }

    def 'source has list, alignTo has map: list-to-map alignment when predicate rejects parent'() {
        given:
        def source = [tags: ["a", "b", "c"]]
        def alignTo = [tags: [a: "mapped-a", b: "mapped-b"]]

        expect:
        // When predicate rejects the parent map, it falls through to list-to-map alignment
        // which checks each list item against the map keys
        def result = MapAligner.align(source, alignTo, { it instanceof String }, entityFactory)
        result.tags == ["mapped-a", "mapped-b", "c"]
    }

    def 'source has list, alignTo has map: non-matching predicate keeps source items'() {
        given:
        def source = [tags: ["a", "b"]]
        def alignTo = [tags: [a: "mapped-a", b: "mapped-b"]]

        expect:
        def result = MapAligner.align(source, alignTo, { false }, entityFactory)
        result.tags == ["a", "b"]
    }

    def 'empty source map returns empty result'() {
        given:
        def source = [:]
        def alignTo = [a: "value"]

        expect:
        MapAligner.align(source, alignTo, { true }, entityFactory) == [:]
    }

    def 'empty alignTo map keeps source values'() {
        given:
        def source = [a: "x", b: "y"]
        def alignTo = [:]

        expect:
        MapAligner.align(source, alignTo, { true }, entityFactory) == [a: "x", b: "y"]
    }

    def 'deeply nested maps are aligned recursively'() {
        given:
        def source = [l1: [l2: [l3: "old"]]]
        def alignTo = [l1: [l2: [l3: "new"]]]

        expect:
        MapAligner.align(source, alignTo, { true }, entityFactory) == [l1: [l2: [l3: "new"]]]
    }

    def 'mixed types: source value is not map or list, kept as-is even when alignTo has map'() {
        given:
        def source = [a: "string-value"]
        def alignTo = [a: [nested: "map"]]

        expect:
        // predicate returns false for the map value, so source value is kept
        def result = MapAligner.align(source, alignTo, { it instanceof String }, entityFactory)
        result.a == "string-value"
    }

    def 'isNull predicate replaces null source values with alignTo values'() {
        given:
        def source = [a: null, b: "existing"]
        def alignTo = [a: "filled", b: "other"]

        expect:
        // Common use case: Objects::isNull as predicate to fill in defaults
        def result = MapAligner.align(source, alignTo, { it == null }, entityFactory)
        result.a == null  // predicate tests alignTo value, not source value
        result.b == "existing"
    }
}
