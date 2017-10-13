package de.ii.xsf.core.web;

import java.util.Set;

/**
 * @author zahnen
 */
public interface JaxRsReg {
    void addService(Object service);
    Set<Object> getResources();
    void addChangeListener(JaxRsChangeListener changeListener);
}
