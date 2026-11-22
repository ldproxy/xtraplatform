/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXMLProvider
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXmlBindXMLProvider
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.ext.Providers
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.nio.charset.StandardCharsets

// Jersey finds two Jackson XML providers: the registered JacksonXmlBindXMLProvider and the plain
// JacksonXMLProvider from META-INF/services. Which one writes a response is not defined. With the
// resolver, both use the mapper that honours the Jakarta XML Binding annotations.
class XmlMapperResolverSpec extends Specification {

    static final String EXPECTED = '<Root xmlns="urn:test" service="S"><Title>t</Title></Root>'

    @XmlRootElement(name = "Root", namespace = "urn:test")
    static class Entity {
        @XmlAttribute(name = "service")
        String getService() { "S" }

        @XmlElement(name = "Title", namespace = "urn:test")
        String getTitle() { "t" }
    }

    static String write(JacksonXMLProvider provider, Providers providers) {
        provider._providers = providers
        def out = new ByteArrayOutputStream()
        provider.writeTo(new Entity(), Entity, Entity, new Annotation[0], MediaType.APPLICATION_XML_TYPE, new MultivaluedHashMap<String, Object>(), out)
        return out.toString(StandardCharsets.UTF_8)
    }

    Providers providers(boolean withResolver) {
        Providers providers = Stub()
        providers.getContextResolver(XmlMapper, _) >> (withResolver ? new XmlMapperResolver() : null)
        return providers
    }

    def 'with the resolver, both providers write the XML binding annotations'() {
        when:
        def plain = write(new JacksonXMLProvider(), providers(true))
        def xmlBind = write(new JacksonXmlBindXMLProvider(), providers(true))

        then:
        plain == EXPECTED
        xmlBind == EXPECTED
    }

    def 'without the resolver, the plain provider ignores the XML binding annotations'() {
        when:
        def plain = write(new JacksonXMLProvider(), providers(false))
        def xmlBind = write(new JacksonXmlBindXMLProvider(), providers(false))

        then:
        plain != EXPECTED
        plain.startsWith('<Entity>')
        xmlBind == EXPECTED
    }
}
