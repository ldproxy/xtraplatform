/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api;

/**
 *
 * @author zahnen
 */
public interface Module {

    /**
     *
     * @return the name of the module
     */
    public String getName();

    /**
     *
     * @return the description of the module
     */
    public String getDescription();
    
}
