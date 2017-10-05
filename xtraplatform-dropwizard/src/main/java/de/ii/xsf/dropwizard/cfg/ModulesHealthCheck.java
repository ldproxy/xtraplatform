/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.cfg;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;

/**
 *
 * @author zahnen
 */
public class ModulesHealthCheck extends HealthCheck {
    /*private final ModulesBundle modules;

    public ModulesHealthCheck(ModulesBundle modules) {
        super();
        this.modules = modules;
    }*/
    
    @Override
    protected Result check() {
        int m = 1;//modules.getModules().size();
        if (m > 0) {
            return Result.healthy("%d modules loaded", m);
        } else {
            return Result.unhealthy("No modules loaded");
        }
    }
}
