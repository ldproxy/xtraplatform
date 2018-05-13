package de.ii.xsf.cfgstore.api;

/**
 * @author zahnen
 */
public class ConfigValueDescriptor {
    private final String label;
    private final String description;
    private final String validator;

    public ConfigValueDescriptor(String label, String description, String validator) {
        this.label = label;
        this.description = description;
        this.validator = validator;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getValidator() {
        return validator;
    }
}
