/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app

import de.ii.xtraplatform.values.domain.Identifier
import spock.lang.Specification

class ValueRevisionsSpec extends Specification {

    ValueRevisions revisions = new ValueRevisions({ ['codelists', 'queries', 'maplibre-styles/v1'] })

    def "no change, no revision"() {
        expect:
        revisions.get() == 0
        revisions.get(['codelists']) == 0
    }

    def "a change counts for the type of the value and for the store"() {
        when:
        revisions.onChange(Identifier.from('cab', 'codelists', 'daraa'))

        then:
        revisions.get(['codelists']) == 1
        revisions.get(['queries']) == 0
        revisions.get() == 1

        when:
        revisions.onChange(Identifier.from('q1', 'queries', 'api'))
        revisions.onChange(Identifier.from('rel', 'codelists', 'daraa'))

        then:
        revisions.get(['codelists']) == 2
        revisions.get(['queries']) == 1
        revisions.get() == 3
    }

    def "a value type with several path segments"() {
        when:
        revisions.onChange(Identifier.from('style', 'maplibre-styles', 'v1', 'api'))

        then:
        revisions.get(['maplibre-styles', 'v1']) == 1
        revisions.get() == 1
    }

    def "an unknown value type has revision 0"() {
        expect:
        revisions.get(null) == 0
        revisions.get(['unknown']) == 0
    }
}
