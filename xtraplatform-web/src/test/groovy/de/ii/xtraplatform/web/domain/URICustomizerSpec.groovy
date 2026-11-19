/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain

import spock.lang.Specification

class URICustomizerSpec extends Specification {

    static final String QUERY = "weglaten=a,b&land=Bosnia%2C%20Herzegovina&f=json"

    def "a query that is not changed is kept as it is"() {
        when:
        def uri = new URICustomizer("https://example.org/items?" + QUERY)

        then:
        uri.toString() == "https://example.org/items?" + QUERY
        uri.build().getRawQuery() == QUERY
    }

    def "a comma that separates items stays a comma when a parameter is removed"() {
        when:
        def uri = new URICustomizer("https://example.org/items?" + QUERY).removeParameters("f")

        then:
        uri.toString() == "https://example.org/items?weglaten=a,b&land=Bosnia%2C%20Herzegovina"
    }

    def "a comma that separates items stays a comma when a parameter is added"() {
        when:
        def uri = new URICustomizer(URI.create("https://example.org/items?" + QUERY))
        uri.setParameter("offset", "10")

        then:
        uri.build().getRawQuery() == QUERY + "&offset=10"
    }

    def "a lower-case encoded comma is a comma within an item, too"() {
        when:
        def uri = new URICustomizer("https://example.org/items?land=Bosnia%2c%20Herzegovina&f=json")
                .removeParameters("f")

        then:
        uri.toString() == "https://example.org/items?land=Bosnia%2C%20Herzegovina"
    }

    def "a copy keeps the encoding of the commas"() {
        when:
        def uri = new URICustomizer("https://example.org/items?" + QUERY).copy()
                .ensureParameter("limit", "10")

        then:
        uri.build().getRawQuery() == QUERY + "&limit=10"
    }

    def "the values of the query parameters are decoded"() {
        when:
        def uri = new URICustomizer("https://example.org/items?" + QUERY)

        then:
        uri.getQueryParams().collect { it.value } == ["a,b", "Bosnia, Herzegovina", "json"]
        uri.getFirstQueryParam("land").value == "Bosnia, Herzegovina"
    }

    def "a value with a comma that is set is written with an unencoded comma"() {
        when:
        def uri = new URICustomizer("https://example.org/items")
        uri.setParameter("bbox", "1,2,3,4")
        uri.setParameter("land", "Bosnia" + URICustomizer.ENCODED_COMMA + " Herzegovina")

        then:
        uri.toString() == "https://example.org/items?bbox=1,2,3,4&land=Bosnia%2C%20Herzegovina"
    }

    def "an encoded comma in the path and the fragment is not changed"() {
        when:
        def uri = new URICustomizer("https://example.org/items/a%2Cb?bbox=1,2,3,4&f=json#x%2Cy")
                .removeParameters("f")

        then:
        uri.toString() == "https://example.org/items/a%2Cb?bbox=1,2,3,4#x%2Cy"
    }

    def "parameters can be removed by a condition on the name"() {
        when:
        def uri = new URICustomizer("?" + QUERY + "&Offset=10")
                .removeParametersIf { it.equalsIgnoreCase("offset") || it == "f" }

        then:
        uri.build().getRawQuery() == "weglaten=a,b&land=Bosnia%2C%20Herzegovina"
    }
}
