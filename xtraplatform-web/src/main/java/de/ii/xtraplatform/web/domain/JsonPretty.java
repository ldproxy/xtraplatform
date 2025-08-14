/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.ext.WriterInterceptorContext;

public interface JsonPretty {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface JsonPrettify {}

  String JSON_PRETTY_HEADER = "x-ldproxy-json-pretty";

  Annotation JSON_PRETTY_ANNOTATION =
      new JsonPrettify() {
        @Override
        public Class<? extends Annotation> annotationType() {
          return JsonPrettify.class;
        }
      };

  static boolean isJsonPretty(WriterInterceptorContext writerInterceptorContext) {
    Object headerValue = writerInterceptorContext.getProperty(JSON_PRETTY_HEADER);

    return headerValue instanceof String && "true".equalsIgnoreCase((String) headerValue);
  }

  static boolean isJsonPretty(Annotation[] annotations) {
    return annotations != null
        && Arrays.stream(annotations).anyMatch(annotation -> annotation instanceof JsonPrettify);
  }

  static void addAnnotation(WriterInterceptorContext writerInterceptorContext) {
    List<Annotation> annotations =
        new ArrayList<>(Arrays.asList(writerInterceptorContext.getAnnotations()));

    annotations.add(JSON_PRETTY_ANNOTATION);

    writerInterceptorContext.setAnnotations(annotations.toArray(Annotation[]::new));
  }
}
