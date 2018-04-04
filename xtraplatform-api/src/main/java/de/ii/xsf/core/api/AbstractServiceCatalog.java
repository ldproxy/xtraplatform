/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
