/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Stereotype;

@Component(propagation = false)
@Provides
@HandlerDeclaration("<callback transition=\"validate\" method=\"onValidate\"></callback>")
@Stereotype
@Target(TYPE)
public @interface EntityComponent {}
