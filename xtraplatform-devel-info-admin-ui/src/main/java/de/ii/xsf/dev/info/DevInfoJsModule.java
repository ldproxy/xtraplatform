/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
