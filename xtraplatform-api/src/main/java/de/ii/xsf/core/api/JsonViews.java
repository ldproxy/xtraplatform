package de.ii.xsf.core.api;

/**
 *
 * @author zahnen
 */
public class JsonViews {
    public static interface FullView extends DefaultView, ConfigurationView {}
    public static interface AdminView extends DefaultView {}
    public static interface ConfigurationView {}
    public static interface DefaultView {}
}
