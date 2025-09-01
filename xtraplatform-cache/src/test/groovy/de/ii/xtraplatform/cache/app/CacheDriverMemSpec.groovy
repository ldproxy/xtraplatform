package de.ii.xtraplatform.cache.app

import spock.lang.Shared
import spock.lang.Specification

import java.sql.ShardingKey

class CacheDriverMemSpec extends Specification{

    @Shared CacheDriverMem cacheDriverMem = new CacheDriverMem()
    def 'Test put get has'() {
        given:
        String key = "key"
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with validator'() {
        given:
        String key = "key"
        String validator = "validator"
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, (String) validator, value)

        then:
        cacheDriverMem.has(key, validator)
        cacheDriverMem.get(key, validator, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with ttl'() {
        given:
        String key = "key"
        int ttl = 60
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, (Object) value, ttl)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with ttl and validator'() {
        given:
        String key = "key"
        String validator = "validator"
        int ttl = 60
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, validator, value, ttl)

        then:
        cacheDriverMem.has(key, validator)
        cacheDriverMem.get(key, validator, String).map(value::equals).orElse(false)
    }

    def 'Test delete'() {
        given:
        String key = "key"
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, value)
        cacheDriverMem.del(key)

        then:
        !cacheDriverMem.has(key)
        !cacheDriverMem.get(key, String).isPresent()
    }
}
