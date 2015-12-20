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
