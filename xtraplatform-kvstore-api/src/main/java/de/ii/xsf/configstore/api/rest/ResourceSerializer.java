/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.core.api.Resource;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author zahnen
 */
public interface ResourceSerializer<T extends Resource> {

    T deserialize(T resource, Reader reader) throws IOException;

    String serializeAdd(T resource) throws IOException;

    String serializeUpdate(T resource) throws IOException;
}
