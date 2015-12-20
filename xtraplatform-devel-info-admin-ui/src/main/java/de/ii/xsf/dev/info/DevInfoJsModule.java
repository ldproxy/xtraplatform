package de.ii.xsf.dev.info;

import de.ii.xsf.core.api.Module;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class DevInfoJsModule implements Module {

    public static final String NAME = "devinfo";
    private static final Logger LOGGER = LoggerFactory.getLogger(DevInfoJsModule.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "";
    }
 
}
