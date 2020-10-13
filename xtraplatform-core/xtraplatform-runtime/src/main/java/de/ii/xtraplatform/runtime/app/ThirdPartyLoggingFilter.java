package de.ii.xtraplatform.runtime.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * @author zahnen
 */
public class ThirdPartyLoggingFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (logger.getName().startsWith("de.ii")) {
            return FilterReply.NEUTRAL;
        }

        return FilterReply.DENY;
    }
}
