/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api;

import java.lang.annotation.Target;

/**
 * @author zahnen
 */
@Target({})
public @interface ConfigPropertyDescriptor {
    String name();

    String label();

    String defaultValue() default "";

    String description() default "";

    String validator() default "";

    boolean hidden() default false;
}
