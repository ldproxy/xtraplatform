/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(TYPE)
public @interface Entity {

  String type();

  SubType[] subTypes() default {};

  Class<?> data();

  boolean auxiliary() default false;

  public @interface SubType {
    String key();

    String value();

    String[] keyAlias() default {};

    String[] valueAlias() default {};
  }
}
