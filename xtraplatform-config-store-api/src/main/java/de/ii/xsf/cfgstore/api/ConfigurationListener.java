package de.ii.xsf.cfgstore.api;

/**
 *
 * @author zahnen
 */
public interface ConfigurationListener<T extends BundleConfigDefault> {

    public void onConfigurationUpdate(T cfg);

}
