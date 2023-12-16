/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.util;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface JacksonModules {
  Logger LOGGER = LoggerFactory.getLogger(JacksonModules.class);

  Module DESERIALIZE_IMMUTABLE_BUILDER_NESTED =
      new Module() {

        @Override
        public String getModuleName() {
          return "DESERIALIZE_MODIFIABLE_MODULE";
        }

        @Override
        public Version version() {
          return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
          context.appendAnnotationIntrospector(
              new NopAnnotationIntrospector() {
                @Override
                public AnnotatedMethod resolveSetterConflict(
                    MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
                  if (isImmutableBuilder(setter1.getDeclaringClass())) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace(
                          "resolving setter conflict for Immutables Builder {} {}",
                          setter1,
                          setter2);
                    }
                    if (isImmutableBuilder(setter1.getRawParameterType(0))) {
                      return setter1;
                    }
                    if (isImmutableBuilder(setter2.getRawParameterType(0))) {
                      return setter2;
                    }
                  } else if (isModifiable(setter1.getDeclaringClass())) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace(
                          "resolving setter conflict for Modifiable {} {}", setter1, setter2);
                    }
                    if (isOptional(setter1.getRawParameterType(0))) {
                      return setter1;
                    }
                    if (isOptional(setter2.getRawParameterType(0))) {
                      return setter2;
                    }
                  }
                  return super.resolveSetterConflict(config, setter1, setter2);
                }
              });
        }

        private boolean isImmutableBuilder(Class<?> clazz) {
          return clazz.getSimpleName().equals("Builder");
          // TODO: annotations not retained
          // && Objects.nonNull(clazz.getAnnotation(Generated.class))
          // && clazz.getAnnotation(Generated.class).generator().equals("Immutables");
        }

        private boolean isModifiable(Class<?> clazz) {
          return clazz.getSimpleName().startsWith("Modifiable");
          // TODO: annotations not retained
          // && Objects.nonNull(clazz.getAnnotation(Generated.class))
          // && clazz.getAnnotation(Generated.class).generator().equals("Immutables");
        }

        private boolean isOptional(Class<?> clazz) {
          return clazz.getSimpleName().equals("Optional");
        }
      };
}
