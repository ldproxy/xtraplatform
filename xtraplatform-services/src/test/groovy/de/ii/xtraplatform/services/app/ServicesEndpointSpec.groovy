package de.ii.xtraplatform.services.app

import spock.lang.Specification

class ServicesEndpointSpec extends Specification {

    def 'version redirect path: #description'() {
        expect:
        ServicesEndpoint.buildVersionRedirectPath(basePath, requestPath, id, 1) == expected

        where:
        description                              | basePath  | requestPath                | id          || expected
        'externalUrl with path prefix'           | '/api/'   | '/buildings'               | 'buildings' || '/api/buildings/v1'
        'externalUrl with path prefix, slash'    | '/api/'   | '/buildings/'              | 'buildings' || '/api/buildings/v1/'
        'prefix without trailing slash'          | '/api'    | '/buildings/'              | 'buildings' || '/api/buildings/v1/'
        'externalUrl without path'               | '/'       | '/roads/'                  | 'roads'     || '/roads/v1/'
        'no externalUrl'                         | ''        | '/roads'                   | 'roads'     || '/roads/v1'
        'null base path'                         | null      | '/roads'                   | 'roads'     || '/roads/v1'
        'sub resource'                           | '/api/'   | '/roads/collections'       | 'roads'     || '/api/roads/v1/collections'
        'id also used as collection id'          | '/api/'   | '/roads/collections/roads' | 'roads'     || '/api/roads/v1/collections/roads'
        'id is prefix of other segment'          | '/api/'   | '/foo/collections/foobar'  | 'foo'       || '/api/foo/v1/collections/foobar'
    }
}
