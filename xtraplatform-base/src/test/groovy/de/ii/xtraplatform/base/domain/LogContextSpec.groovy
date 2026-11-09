package de.ii.xtraplatform.base.domain

import org.slf4j.Logger
import spock.lang.Specification

class LogContextSpec extends Specification {

    def "#method logs the message of the throwable at its level"() {
        given:
        Logger logger = Mock()
        def throwable = new IllegalStateException("boom")

        when:
        LogContext."$method"(logger, throwable, "Could not do {}", "it")

        then:
        (level == 'warn' ? 1 : 0) * logger.warn("Could not do {}: {}", "it", "boom")
        (level == 'info' ? 1 : 0) * logger.info("Could not do {}: {}", "it", "boom")
        (level == 'debug' ? 1 : 0) * logger.debug("Could not do {}: {}", "it", "boom")
        0 * logger.error(*_)

        where:
        method         || level
        'errorAsWarn'  || 'warn'
        'errorAsInfo'  || 'info'
        'errorAsDebug' || 'debug'
    }
}
