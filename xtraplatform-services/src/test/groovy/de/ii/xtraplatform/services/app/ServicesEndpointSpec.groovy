package de.ii.xtraplatform.services.app

import spock.lang.Specification

class ServicesEndpointSpec extends Specification {

    def 'version redirect path: #description'() {
        expect:
        ServicesEndpoint.buildVersionRedirectPath(basePath, requestPath, id, 1) == expected

        where:
        description                              | basePath              | requestPath                      | id              || expected
        'externalUrl with path prefix'           | '/xtraserver-webapi/' | '/alkis-vereinf'                 | 'alkis-vereinf' || '/xtraserver-webapi/alkis-vereinf/v1'
        'externalUrl with path prefix, slash'    | '/xtraserver-webapi/' | '/alkis-vereinf/'                | 'alkis-vereinf' || '/xtraserver-webapi/alkis-vereinf/v1/'
        'prefix without trailing slash'          | '/xtraserver-webapi'  | '/alkis-vereinf/'                | 'alkis-vereinf' || '/xtraserver-webapi/alkis-vereinf/v1/'
        'externalUrl without path'               | '/'                   | '/welterbe/'                     | 'welterbe'      || '/welterbe/v1/'
        'no externalUrl'                         | ''                    | '/welterbe'                      | 'welterbe'      || '/welterbe/v1'
        'null base path'                         | null                  | '/welterbe'                      | 'welterbe'      || '/welterbe/v1'
        'sub resource'                           | '/api/'               | '/welterbe/collections'          | 'welterbe'      || '/api/welterbe/v1/collections'
        'id also used as collection id'          | '/api/'               | '/denkmale/collections/denkmale' | 'denkmale'      || '/api/denkmale/v1/collections/denkmale'
        'id is prefix of other segment'          | '/api/'               | '/foo/collections/foobar'        | 'foo'           || '/api/foo/v1/collections/foobar'
    }
}
