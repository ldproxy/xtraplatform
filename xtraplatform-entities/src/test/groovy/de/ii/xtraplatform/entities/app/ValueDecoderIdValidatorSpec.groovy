package de.ii.xtraplatform.entities.app

import com.fasterxml.jackson.databind.ObjectMapper
import de.ii.xtraplatform.entities.domain.EntityData
import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class ValueDecoderIdValidatorSpec extends Specification {

    def validator = new ValueDecoderIdValidator()
    def objectMapper = new ObjectMapper()

    def 'identifier id matches data id returns data'() {
        given:
        def identifier = Identifier.from("my-service", "services")
        def data = Stub(EntityData) {
            getId() >> "my-service"
        }

        expect:
        validator.process(identifier, "{}".bytes, objectMapper, data, false) == data
    }

    def 'identifier id does not match data id returns null'() {
        given:
        def identifier = Identifier.from("my-service", "services")
        def data = Stub(EntityData) {
            getId() >> "other-service"
        }

        expect:
        validator.process(identifier, "{}".bytes, objectMapper, data, false) == null
    }
}
