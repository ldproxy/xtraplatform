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

// @Component(propagation = false)
// @Provides
// customization in HandlerDeclarationVisitor needed for multiple callbacks
// @HandlerDeclaration("<callbacks><callback transition=\"validate\"
// method=\"onValidate\"></callback><callback transition=\"invalidate\"
// method=\"onInvalidate\"></callback></callbacks>")
// @Stereotype
@Target(TYPE)
public @interface EntityComponent {}
