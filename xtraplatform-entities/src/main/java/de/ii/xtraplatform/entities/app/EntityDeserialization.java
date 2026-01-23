/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.NameTransformer;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// TODO:
public class EntityDeserialization {

  public static final Module DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER =
      new SimpleModule()
          .setDeserializerModifier(
              new BeanDeserializerModifier() {
                @Override
                public JsonDeserializer<?> modifyDeserializer(
                    DeserializationConfig config,
                    BeanDescription beanDesc,
                    JsonDeserializer<?> deserializer) {
                  if (deserializer instanceof BeanDeserializer) {

                    Optional<BeanPropertyDefinition> propertyDefinition =
                        beanDesc.findProperties().stream()
                            .filter(
                                beanPropertyDefinition ->
                                    beanPropertyDefinition
                                        .getRawPrimaryType()
                                        .getName()
                                        .endsWith("MapWrapper$Builder"))
                            .findFirst();

                    if (propertyDefinition.isPresent()) {
                      return new ImmutableBuilderMapWrapperDeserializer(
                          (BeanDeserializer) deserializer, propertyDefinition.get().getName());
                    }
                  }
                  return super.modifyDeserializer(config, beanDesc, deserializer);
                }
              });

  public static class ImmutableBuilderMapWrapperDeserializer extends BeanDeserializer {

    private static final long serialVersionUID = 1L;
    private final String wrappedPropertyName;

    ImmutableBuilderMapWrapperDeserializer(
        BeanDeserializer defaultDeserializer, String wrappedPropertyName) {
      super(defaultDeserializer);
      this.wrappedPropertyName = wrappedPropertyName;
    }

    private List<SettableBeanProperty> remapBeanProperties(
        BeanDeserializer unwrapping, SettableBeanProperty mapProp, String wrappedPropertyName) {
      Iterable<SettableBeanProperty> iterable = unwrapping::properties;
      return StreamSupport.stream(iterable.spliterator(), false)
          .map(prop -> prop == mapProp ? mapProp.withSimpleName(wrappedPropertyName) : prop)
          .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
      super.resolve(ctxt);

      SettableBeanProperty prop = findProperty(wrappedPropertyName);
      if (prop != null) {
        NameTransformer xform = NameTransformer.simpleTransformer("", "");

        if (_unwrappedPropertyHandler == null) {
          _unwrappedPropertyHandler = new UnwrappedPropertyHandler();
        }

        JsonDeserializer<Object> orig = prop.getValueDeserializer();
        JsonDeserializer<Object> unwrapping = orig.unwrappingDeserializer(xform);
        if (!Objects.equals(unwrapping, orig)
            && unwrapping != null
            && unwrapping instanceof BeanDeserializer) {
          SettableBeanProperty mapProp = ((BeanDeserializer) unwrapping).findProperty("map");
          if (mapProp != null) {
            List<SettableBeanProperty> beanProperties =
                remapBeanProperties((BeanDeserializer) unwrapping, mapProp, wrappedPropertyName);
            BeanPropertyMap beanPropertyMap =
                new BeanPropertyMap(
                    false, beanProperties, Collections.<String, List<PropertyName>>emptyMap());
            unwrapping = ((BeanDeserializer) unwrapping).withBeanProperties(beanPropertyMap);
          }

          prop = prop.withValueDeserializer(unwrapping);
          _unwrappedPropertyHandler.addProperty(prop);
          _beanProperties.remove(prop);
        }

        prop = prop.withValueDeserializer(unwrapping);
        _unwrappedPropertyHandler.addProperty(prop);
        _beanProperties.remove(prop);
      }
    }
  }

  public static final Module DESERIALIZE_API_BUILDINGBLOCK_MIGRATION =
      new Module() {

        @Override
        public String getModuleName() {
          return "DESERIALIZE_API_BUILDINGBLOCK_MIGRATION";
        }

        @Override
        public Version version() {
          return Version.unknownVersion();
        }

        @Override
        @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CloseResource"})
        public void setupModule(Module.SetupContext context) {
          context.addDeserializationProblemHandler(
              new DeserializationProblemHandler() {
                @Override
                public JavaType handleMissingTypeId(
                    DeserializationContext ctxt,
                    JavaType baseType,
                    TypeIdResolver idResolver,
                    String failureMsg)
                    throws IOException {
                  if (!failureMsg.contains("'buildingBlock'")) {
                    return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
                  }

                  JsonParser p = ctxt.getParser();

                  JsonLocation currentLocation = p.getCurrentLocation();
                  byte[] sourceRef = (byte[]) currentLocation.getSourceRef();

                  JsonParser parser2 = p.getCodec().getFactory().createParser(sourceRef);
                  parser2.nextToken();
                  parser2.nextToken();
                  parser2.nextToken();

                  // but first, sanity check to ensure we have START_OBJECT or FIELD_NAME
                  JsonToken currentToken = parser2.nextToken();
                  if (currentToken == JsonToken.START_OBJECT) {
                    currentToken = parser2.nextToken();
                  } else if (
                  /*t == JsonToken.START_ARRAY ||*/ currentToken != JsonToken.FIELD_NAME) {
                    /* This is most likely due to the fact that not all Java types are
                     * serialized as JSON Objects; so if "as-property" inclusion is requested,
                     * serialization of things like Lists must be instead handled as if
                     * "as-wrapper-array" was requested.
                     * But this can also be due to some custom handling: so, if "defaultImpl"
                     * is defined, it will be asked to handle this case.
                     */
                    return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
                  }
                  // Ok, let's try to find the property. But first, need token buffer...

                  long currentLine = parser2.getCurrentLocation().getLineNr();
                  long currentColumn = parser2.getCurrentLocation().getColumnNr();
                  long line = currentLocation.getLineNr();
                  long column = currentLocation.getColumnNr();

                  String lastExtensionType = null;

                  while (currentLine < line || currentColumn < column) {

                    for (;
                        currentToken != JsonToken.END_OBJECT;
                        currentToken = parser2.nextToken()) {
                      if (JsonToken.FIELD_NAME == currentToken
                          && "extensionType".equals(parser2.getCurrentName())) {
                        lastExtensionType = parser2.getValueAsString();
                      }
                    }

                    currentLine = parser2.getCurrentLocation().getLineNr();
                    currentColumn = parser2.getCurrentLocation().getColumnNr();
                    currentToken = parser2.nextToken();
                  }

                  if (currentLine == line
                      && currentColumn == column
                      && Objects.nonNull(lastExtensionType)) {
                    return idResolver.typeFromId(ctxt, lastExtensionType);
                  }

                  return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
                }
              });
        }
      };
}
