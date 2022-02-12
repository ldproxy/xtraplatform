/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.dropwizard.jackson.CaffeineModule;
import io.dropwizard.jackson.FuzzyEnumModule;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
// TODO: to store, only use for de/serialization
// TODO: find all usages, is a generic version needed? also see Jackson.newObjectMapper()
@Singleton
@AutoBind
public class JacksonProvider implements Jackson {

  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonProvider.class);

  private final DynamicHandlerInstantiator dynamicHandlerInstantiator;
  private final ObjectMapper jsonMapper;
  private final BiMap<Class<?>, String> mapping;

  @Inject
  public JacksonProvider(Set<JacksonSubTypeIds> subTypeIds) {
    this.dynamicHandlerInstantiator = new DynamicHandlerInstantiator();
    this.jsonMapper = configureMapper(new ObjectMapper());
    this.mapping = HashBiMap.create();
    subTypeIds.forEach(ids -> mapping.putAll(ids.getMapping()));
  }

  private ObjectMapper configureMapper(ObjectMapper mapper) {
    return (ObjectMapper)
        mapper
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .registerModule(new CaffeineModule())
            .registerModule(
                new AfterburnerModule()) // TODO: suppresses reflections warnings, but does not work
            // with osgi .setUseValueClassLoader(false))
            .registerModule(new FuzzyEnumModule())
            .registerModule(new JavaTimeModule())
            .setDefaultMergeable(false)
            .setHandlerInstantiator(dynamicHandlerInstantiator);
  }

  @Override
  public ObjectMapper getDefaultObjectMapper() {
    return jsonMapper;
  }

  @Override
  public ObjectMapper getNewObjectMapper(JsonFactory jsonFactory) {
    return configureMapper(new ObjectMapper(jsonFactory));
  }

  // TODO: needs to be in domain to access this
  public class DynamicTypeIdResolver implements TypeIdResolver {

    private JavaType mBaseType;

    public DynamicTypeIdResolver(JavaType mBaseType) {
      this.mBaseType = mBaseType;
    }

    @Override
    public void init(JavaType baseType) {
      mBaseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
      return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromBaseType() {
      return idFromValueAndType(null, mBaseType.getRawClass());
    }

    @Override
    public String getDescForKnownTypeIds() {
      return null;
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
      if (mapping.containsKey(suggestedType)) {
        return mapping.get(suggestedType);
      }
      Class<?> aClass = value.getClass();
      while (aClass != null) {
        if (mapping.containsKey(aClass)) {
          return mapping.get(aClass);
        }
        aClass = aClass.getSuperclass();
      }

      return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
      if (mapping.inverse().containsKey(id)) {
        Class<?> clazz = mapping.inverse().get(id);
        JavaType javaType =
            TypeFactory.defaultInstance().constructSpecializedType(mBaseType, clazz);
        return javaType;
      }

      return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
      return JsonTypeInfo.Id.CUSTOM;
    }
  }

  class DynamicHandlerInstantiator extends HandlerInstantiator {

    private final Map<String, DynamicTypeIdResolver> typeIdResolvers;

    DynamicHandlerInstantiator() {
      this.typeIdResolvers = new HashMap<>();
    }

    @Override
    public JsonDeserializer<?> deserializerInstance(
        DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
      return null;
    }

    @Override
    public KeyDeserializer keyDeserializerInstance(
        DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
      return null;
    }

    @Override
    public JsonSerializer<?> serializerInstance(
        SerializationConfig config, Annotated annotated, Class<?> serClass) {
      return null;
    }

    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(
        MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
      return null;
    }

    @Override
    public TypeIdResolver typeIdResolverInstance(
        MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
      if (resolverClass.equals(DynamicTypeIdResolver.class)) {
        typeIdResolvers.putIfAbsent(
            annotated.getName(), new DynamicTypeIdResolver(annotated.getType()));
        // LOGGER.debug("DynamicHandlerInstantiator typeIdResolverInstance {} {}",
        // annotated.getName(), typeIdResolvers.get(annotated.getName()));
        return typeIdResolvers.get(annotated.getName());
      }
      return null;
    }
  }
}
