/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Custom ModelConverter that creates a custom schema for AtomicInteger and AtomicLong types
 */
public class CustomModelConverter implements ModelConverter {
  private static final Schema SCHEMA_ATOMIC_INTEGER = new Schema().type("integer").format("int32");
  private static final Schema SCHEMA_ATOMIC_LONG = new Schema().type("integer").format("int64");

  @Override
  public Schema resolve(
      AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
    Type type = annotatedType.getType();
    if (type instanceof SimpleType) {
      SimpleType simpleType = (SimpleType) type;

      if (AtomicInteger.class.isAssignableFrom(simpleType.getRawClass())
          || OptionalInt.class.isAssignableFrom(simpleType.getRawClass())) {
        return SCHEMA_ATOMIC_INTEGER;
      } else if (AtomicLong.class.isAssignableFrom(simpleType.getRawClass())) {
        return SCHEMA_ATOMIC_LONG;
      }
    }

    // It's needed to follow chain for unresolved types
    if (chain.hasNext()) {
      return chain.next().resolve(annotatedType, context, chain);
    }
    return null;
  }
}
