/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 *
 * @author zahnen
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Entity {
    String type();
    String subType() default "";
    Class<?> dataClass();

    String TYPE_KEY = "type";
    String SUB_TYPE_KEY = "subType";
    String DATA_CLASS_KEY = "dataClass";
    String DATA_KEY = "data";
}
