package de.ii.xsf.core.api;

import de.ii.xsf.core.api.esri.CurrentVersion;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
public abstract class AbstractServiceCatalog implements ServiceCatalog {

    @Override
    public double getCurrentVersion() {
        return CurrentVersion.NUMBER;
    }

    @Override
    public List<String> getFolders() {
        return new ArrayList();
    }
}
