/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.MergingSettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.xtraplatform.entity.api.RemoveEntityData;
import de.ii.xtraplatform.kvstore.api.rest.ResourceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author zahnen
 */
public class EntitySerializer implements ResourceSerializer<RemoveEntityData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySerializer.class);

    protected final ObjectMapper jsonMapper;
    protected final ObjectMapper jsonMapperPartial;
    protected final ObjectMapper jsonMapperMerge;

    public EntitySerializer(ObjectMapper jsonMapper) {
        //TODO: only immutables should be serialized by default mapper, then this would be unnecessary
        jsonMapper.registerModule(SERIALIZE_PARTIAL_MODULE);

        this.jsonMapper = jsonMapper.copy()
                                    .registerModule(DESERIALIZE_MODIFIABLE_MODULE)
                                    .registerModule(DESERIALIZE_MODIFIABLE_MODULE2)
                                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //.setDefaultMergeable(true)
        ;//.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP));

        this.jsonMapperPartial = this.jsonMapper.copy()
                                                ;//.registerModule(SERIALIZE_PARTIAL_MODULE);
        this.jsonMapperMerge = this.jsonMapper.copy()
                                              .setDefaultMergeable(true);
    }

    @Override
    public RemoveEntityData deserialize(RemoveEntityData resource, Reader reader) throws IOException {
        jsonMapper.readerForUpdating(resource)
                  .readValue(reader);
        return resource;
    }

    @Override
    public RemoveEntityData deserialize(String id, Class<?> clazz, Reader reader) throws IOException {
        return (RemoveEntityData) jsonMapper.readValue(reader, clazz);
    }

    @Override
    public ObjectNode deserializeMerge(Reader reader) throws IOException {
        return (ObjectNode) jsonMapper.readTree(reader);
    }

    @Override
    public String serializeAdd(RemoveEntityData resource) throws IOException {
        return jsonMapperPartial.writer()
                                .writeValueAsString(resource);
    }

    @Override
    public String serializeUpdate(RemoveEntityData resource) throws IOException {
        return jsonMapperPartial.writeValueAsString(resource);
    }

    @Override
    public String serializeMerge(RemoveEntityData resource) throws IOException {
        return jsonMapperPartial.writer()
                                .writeValueAsString(resource);
    }

    @Override
    public Optional<RemoveEntityData> deserializePartial(Class<?> clazz, Reader reader) throws IOException {
        return Optional.of((RemoveEntityData) jsonMapperPartial.readValue(reader, clazz));
    }

    @Override
    public RemoveEntityData mergePartial(RemoveEntityData resource, String partial) throws IOException {
        return jsonMapperMerge.readerForUpdating(resource)
                              .readValue(partial);
    }

    @Override
    public RemoveEntityData mergePartial(RemoveEntityData resource, Reader reader) throws IOException {
        return jsonMapperMerge.readerForUpdating(resource)
                              .readValue(reader);
    }

    private static final SimpleModule SERIALIZE_PARTIAL_MODULE = new SimpleModule()
            .setSerializerModifier(new BeanSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                    return beanProperties.stream()
                                         .map(bpw -> new BeanPropertyWriter(bpw) {
                                             @Override
                                             public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
                                                 if (bean.getClass()
                                                         .getSimpleName()
                                                         .startsWith("Modifiable")) {
                                                     if (this.getName()
                                                             .equals("initialized")) {
                                                         return;
                                                     }
                                                     try {
                                                         boolean isSet = true;
                                                         try {
                                                             Method method = bean.getClass()
                                                                                 .getMethod(this.getName() + "IsSet");

                                                             isSet = (boolean) method.invoke(bean, (Object[]) null);
                                                         } catch (NoSuchMethodException e) {
                                                             //ignore
                                                         }

                                                         if (isSet) {
                                                             super.serializeAsField(bean, gen, prov);
                                                         } else {
                                                             LOGGER.trace("ignoring unset field '{}' of {} instance", this.getName(), bean.getClass()
                                                                                                                                                              .getName());
                                                         }

                                                     } catch (Throwable e) {
                                                         //ignore
                                                     }
                                                 } else {
                                                     super.serializeAsField(bean, gen, prov);
                                                 }
                                             }
                                         })
                                         .collect(Collectors.toList());
                }
            });


    private static final Module DESERIALIZE_MODIFIABLE_MODULE2 = new SimpleModule()
            .setDeserializerModifier(new BeanDeserializerModifier() {
                @Override
                public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                    if (deserializer instanceof BeanDeserializer && beanDesc.getBeanClass()
                                                                            .getSimpleName()
                                                                            .startsWith("Modifiable")) {
                        LOGGER.trace("catch getter exceptions for {}", beanDesc.getBeanClass());
                        Iterable<SettableBeanProperty> iterable = ((BeanDeserializer) deserializer)::properties;
                        return new BeanDeserializer((BeanDeserializer) deserializer, new BeanPropertyMap(true, StreamSupport.stream(iterable.spliterator(), false)
                                                                                                                            .map(settableBeanProperty -> {

                                                                                                                                return settableBeanProperty;
                                                                                                                            })
                                                                                                                            .collect(Collectors.toList()))) {
                            @Override
                            protected SettableBeanProperty _resolveMergeAndNullSettings(DeserializationContext ctxt, SettableBeanProperty prop, PropertyMetadata propMetadata) throws JsonMappingException {
                                SettableBeanProperty settableBeanProperty = super._resolveMergeAndNullSettings(ctxt, prop, propMetadata);
                                if (settableBeanProperty instanceof MergingSettableBeanProperty) {
                                    LOGGER.debug("catch getter exception for {}", settableBeanProperty.getFullName());
                                    return new MergingSettableBeanProperty((MergingSettableBeanProperty) settableBeanProperty, settableBeanProperty) {
                                        @Override
                                        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
                                            LOGGER.debug("MERGING {}", instance.getClass());
                                            //super.deserializeAndSet(p, ctxt, instance);
                                            Object oldValue = null;
                                            try {
                                                oldValue = _accessor.getValue(instance);
                                            } catch (Throwable e) {
                                                LOGGER.trace("ignoring unset field '{}' of {} instance", this.getName(), this.getDeclaringClass()
                                                                                                                                                 .getClass()
                                                                                                                                                 .getName());
                                            }

                                            Object newValue;
                                            // 20-Oct-2016, tatu: Couple of possibilities of how to proceed; for
                                            //    now, default to "normal" handling without merging
                                            if (oldValue == null) {
                                                newValue = delegate.deserialize(p, ctxt);
                                            } else {
                                                //newValue = delegate.deserializeWith(p, ctxt, oldValue);
                                                newValue = deserializeWith2(p, ctxt, oldValue);
                                            }
                                            if (newValue != oldValue) {
                                                // 18-Apr-2017, tatu: Null handling should occur within delegate, which may
                                                //     set/skip/transform it, or throw an exception.
                                                delegate.set(instance, newValue);
                                            }
                                        }

                                        public final Object deserializeWith2(JsonParser p, DeserializationContext ctxt,
                                                                             Object toUpdate) throws IOException {
                                            // 20-Oct-2016, tatu: Not 100% sure what to do; probably best to simply return
                                            //   null value and let caller decide what to do.
                                            if (p.hasToken(JsonToken.VALUE_NULL)) {
                                                // ... except for "skip nulls" case which should just do that:
                                                if (NullsConstantProvider.isSkipper(_nullProvider)) {
                                                    return toUpdate;
                                                }
                                                return _nullProvider.getNullValue(ctxt);
                                            }
                                            // 20-Oct-2016, tatu: Also tricky -- for now, report an error
                                            if (_valueTypeDeserializer != null) {
                                                if (_valueTypeDeserializer instanceof AsPropertyTypeDeserializer) {
                                                    AsPropertyTypeDeserializer asPropertyTypeDeserializer = new AsPropertyTypeDeserializer((AsPropertyTypeDeserializer) _valueTypeDeserializer, this) {
                                                        @Override
                                                        protected Object _deserializeTypedForId(JsonParser p, DeserializationContext ctxt, TokenBuffer tb) throws IOException {
                                                            String typeId = p.getText();
                                                            JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
                                                            if (_typeIdVisible) { // need to merge id back in JSON input?
                                                                if (tb == null) {
                                                                    tb = new TokenBuffer(p, ctxt);
                                                                }
                                                                tb.writeFieldName(p.getCurrentName());
                                                                tb.writeString(typeId);
                                                            }
                                                            if (tb != null) { // need to put back skipped properties?
                                                                // 02-Jul-2016, tatu: Depending on for JsonParserSequence is initialized it may
                                                                //   try to access current token; ensure there isn't one
                                                                p.clearCurrentToken();
                                                                p = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
                                                            }
                                                            // Must point to the next value; tb had no current, jp pointed to VALUE_STRING:
                                                            p.nextToken(); // to skip past String value
                                                            // deserializer should take care of closing END_OBJECT as well
                                                            return deser.deserialize(p, ctxt, toUpdate);
                                                        }
                                                    };

                                                    return _valueDeserializer.deserializeWithType(p, ctxt, asPropertyTypeDeserializer);
                                                }
                                                ctxt.reportBadDefinition(getType(),
                                                        String.format("Cannot merge polymorphic property '%s'",
                                                                getName()));
                                            }
                                            return _valueDeserializer.deserialize(p, ctxt, toUpdate);
                                        }
                                    };
                                }
                                return settableBeanProperty;
                            }
                        };
                    }
                    return super.modifyDeserializer(config, beanDesc, deserializer);
                }

                @Override
                public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc, List<BeanPropertyDefinition> propDefs) {
                    return propDefs.stream()
                                   .map(bpd -> new MyPOJOPropertyBuilder((POJOPropertyBuilder) bpd, bpd.getFullName()))
                                   .collect(Collectors.toList());
                }
            });

    static class MyPOJOPropertyBuilder extends POJOPropertyBuilder {
        MyPOJOPropertyBuilder(POJOPropertyBuilder src, PropertyName newName) {
            super(src, newName);
        }

        @Override
        public AnnotatedMethod getGetter() {
            return super.getGetter();
        }
    }

    private static final Module DESERIALIZE_MODIFIABLE_MODULE = new Module() {

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
            context.appendAnnotationIntrospector(new NopAnnotationIntrospector() {
                @Override
                public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
                    if (isImmutableBuilder(setter1.getDeclaringClass())) {
                        LOGGER.trace("resolving setter conflict for Immutables Builder {} {}", setter1, setter2);
                        if (isImmutableBuilder(setter1.getRawParameterType(0))) {
                            return setter1;
                        }
                        if (isImmutableBuilder(setter2.getRawParameterType(0))) {
                            return setter2;
                        }
                    }
                    else if (setter1.getDeclaringClass()
                               .getSimpleName()
                               .startsWith("Modifiable")) {
                        LOGGER.trace("resolving setter conflict for Modifiable {} {}", setter1, setter2);
                        if (setter1.getRawParameterType(0)
                                   .equals(Optional.class)) {
                            return setter1;
                        }
                        if (setter2.getRawParameterType(0)
                                   .equals(Optional.class)) {
                            return setter2;
                        }
                    }
                    return super.resolveSetterConflict(config, setter1, setter2);
                }
            });
        }

        private boolean isImmutableBuilder(Class<?> clazz) {
            return clazz.getSimpleName()
                        .equals("Builder");
            //TODO: annotations not retained
            //&& Objects.nonNull(clazz.getAnnotation(Generated.class))
            //&& clazz.getAnnotation(Generated.class).generator().equals("Immutables");
        }
    };
}
