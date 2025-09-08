package de.ii.xtraplatform.cache.app

import spock.lang.Shared
import spock.lang.Specification

import java.sql.ShardingKey

class CacheDriverMemSpec extends Specification{

    @Shared CacheDriverMem cacheDriverMem = new CacheDriverMem()
    def 'Test put get has String'() {
        given:
        String key = "key"
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test put get has int'() {
        given:
        String key = "key"
        int value = 11

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, Integer).map(value::equals).orElse(false)
    }

    def 'Test put get has double'() {
        given:
        String key = "key"
        double value = 11

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, Double).map(value::equals).orElse(false)
    }

    def 'Test put get has boolean'() {
        given:
        String key = "key"
        boolean value = 11

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, Boolean).map(value::equals).orElse(false)
    }

    def 'Test put get has List'() {
        given:
        String key = "key"
        List<Integer> value = List.of(10,20,30)

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, List<Integer>).map(value::equals).orElse(false)
    }

    def 'Test put get has Map'() {
        given:
        String key = "key"
        Map<Integer, String> value = Map.of(1, "One", 2, "Two")

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, Map<Integer, String>).map(value::equals).orElse(false)
    }

    def 'Test put get has int'() {
        given:
        String key = "key"
        int value = 11

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, Integer).map(value::equals).orElse(false)
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

    def 'Test put get has with ttl and validator STRING'() {
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

    def 'Test empty String key'() {
        given:
        String key = ""
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test empty String value'() {
        given:
        String key = "key"
        String value = ""

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test null key'() {
        given:
        String key = null
        String value = "Test Object String"

        when:
        cacheDriverMem.put(key, value)

        then:
        thrown(NullPointerException)
    }

    def 'Test null value'() {
        given:
        String key = "key"
        String value = null

        when:
        cacheDriverMem.put(key, value)

        then:
        cacheDriverMem.has(key)
        cacheDriverMem.get(key, String).isEmpty()
    }
}
