package de.ii.xtraplatform.runtime.domain;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ii.xtraplatform.runtime.app.ThirdPartyLoggingFilter;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.LoggingUtil;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = XtraPlatformLoggingFactory.class)
public class XtraPlatformLoggingFactory extends DefaultLoggingFactory {
    private static final ThirdPartyLoggingFilter thirdPartyLoggingFilter = new ThirdPartyLoggingFilter();

    private boolean showThirdPartyLoggers;

    public XtraPlatformLoggingFactory() {
        super();
        this.showThirdPartyLoggers = false;
    }

    @Override
    public void configure(MetricRegistry metricRegistry, String name) {
        super.configure(metricRegistry, name);

        LoggingUtil.getLoggerContext().resetTurboFilterList();

        if (!showThirdPartyLoggers) {
            LoggingUtil.getLoggerContext()
                       .addTurboFilter(thirdPartyLoggingFilter);
        }
    }

    @JsonProperty("showThirdPartyLoggers")
    public boolean getThirdPartyLogging() {
        return showThirdPartyLoggers;
    }

    @JsonProperty("showThirdPartyLoggers")
    public void setThirdPartyLogging(boolean showThirdPartyLoggers) {
        this.showThirdPartyLoggers = showThirdPartyLoggers;
    }

    @JsonProperty("type")
    public void setType(String type) {
    }
}
