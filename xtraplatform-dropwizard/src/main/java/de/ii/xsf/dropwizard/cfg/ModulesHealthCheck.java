/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
