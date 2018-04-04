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
public class JsonViews {
    public static interface FullView extends DefaultView, ConfigurationView {}
    public static interface AdminView extends DefaultView {}
    public static interface ConfigurationView {}
    public static interface DefaultView {}
}
