package de.ii.xtraplatform.cache.app

import de.ii.xtraplatform.base.domain.AppConfiguration
import de.ii.xtraplatform.base.domain.AppContext
import de.ii.xtraplatform.base.domain.Constants
import de.ii.xtraplatform.base.domain.JacksonProvider
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path


class CacheDriverFsSpec extends Specification{

    @Shared CacheDriverFs cacheDriverFs = new CacheDriverFs(new AppContext() {
        @Override
        String getName() {
            return null
        }

        @Override
        String getVersion() {
            return null
        }

        @Override
        Constants.ENV getEnvironment() {
            return null
        }

        @Override
        Path getDataDir() {
            return null
        }

        @Override
        Path getTmpDir() {
            return Files.createTempDirectory("foo")
        }

        @Override
        AppConfiguration getConfiguration() {
            return null
        }

        @Override
        URI getUri() {
            return null
        }

        @Override
        String getInstanceName() {
            return null
        }
    }, new JacksonProvider(Set::of))

    def 'Test put get has'() {
        given:
        String key = "key"
        String value = "Test Object String"

        when:
        cacheDriverFs.put(key, value)

        then:
        cacheDriverFs.has(key)
        cacheDriverFs.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with validator'() {
        given:
        String key = "key"
        String validator = "validator"
        String value = "Test Object String"

        when:
        cacheDriverFs.put(key, (String) validator, value)

        then:
        cacheDriverFs.has(key, validator)
        cacheDriverFs.get(key, validator, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with ttl'() {
        given:
        String key = "key"
        int ttl = 60
        String value = "Test Object String"

        when:
        cacheDriverFs.put(key, (Object) value, ttl)

        then:
        cacheDriverFs.has(key)
        cacheDriverFs.get(key, String).map(value::equals).orElse(false)
    }

    def 'Test put get has with ttl and validator'() {
        given:
        String key = "key"
        String validator = "validator"
        int ttl = 60
        String value = "Test Object String"

        when:
        cacheDriverFs.put(key, validator, value, ttl)

        then:
        cacheDriverFs.has(key, validator)
        cacheDriverFs.get(key, validator, String).map(value::equals).orElse(false)
    }

    def 'Test delete'() {
        given:
        String key = "key"
        String value = "Test Object String"

        when:
        cacheDriverFs.put(key, value)
        cacheDriverFs.del(key)

        then:
        !cacheDriverFs.has(key)
        !cacheDriverFs.get(key, String).isPresent()
    }
}
