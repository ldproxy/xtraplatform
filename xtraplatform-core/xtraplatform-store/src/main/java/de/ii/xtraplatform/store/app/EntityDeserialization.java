package de.ii.xtraplatform.store.app;

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
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

//TODO:
public class EntityDeserialization {
    private final static Logger LOGGER = LoggerFactory.getLogger(EntityDeserialization.class);


    public static final Module DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER = new SimpleModule().setDeserializerModifier(new BeanDeserializerModifier() {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                                                      JsonDeserializer<?> deserializer) {
            if (deserializer instanceof BeanDeserializer) {

                Optional<BeanPropertyDefinition> propertyDefinition = beanDesc.findProperties()
                                                                              .stream()
                                                                              .filter(beanPropertyDefinition -> beanPropertyDefinition.getRawPrimaryType()
                                                                                                                                      .getName()
                                                                                                                                      .endsWith("MapWrapper$Builder"))
                                                                              .findFirst();

                if (propertyDefinition.isPresent()) {
                    return new ImmutableBuilderMapWrapperDeserializer((BeanDeserializer) deserializer, propertyDefinition.get()
                                                                                                                         .getName());
                }
            }
            return super.modifyDeserializer(config, beanDesc, deserializer);
        }
    });

    public static class ImmutableBuilderMapWrapperDeserializer extends BeanDeserializer {

        private final String wrappedPropertyName;

        ImmutableBuilderMapWrapperDeserializer(BeanDeserializer defaultDeserializer, String wrappedPropertyName) {
            super(defaultDeserializer);
            this.wrappedPropertyName = wrappedPropertyName;
        }

        @Override
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
                if (unwrapping != orig && unwrapping != null && unwrapping instanceof BeanDeserializer) {
                    SettableBeanProperty mapProp = ((BeanDeserializer) unwrapping).findProperty("map");
                    if (mapProp != null) {
                        Iterable<SettableBeanProperty> iterable = ((BeanDeserializer) unwrapping)::properties;
                        List<SettableBeanProperty> beanProperties = StreamSupport.stream(iterable.spliterator(), false)
                                                                                 .map(settableBeanProperty -> {
                                                                                     if (settableBeanProperty == mapProp) {
                                                                                         return mapProp.withSimpleName(wrappedPropertyName);
                                                                                     }
                                                                                     return settableBeanProperty;
                                                                                 })
                                                                                 .collect(Collectors.toList());
                        BeanPropertyMap beanPropertyMap = new BeanPropertyMap(false, beanProperties, Collections.<String, List<PropertyName>>emptyMap());
                        unwrapping = ((BeanDeserializer) unwrapping).withBeanProperties(beanPropertyMap);
                    }

                    prop = prop.withValueDeserializer(unwrapping);
                    _unwrappedPropertyHandler.addProperty(prop);
                    _beanProperties.remove(prop);
                }
            }
        }
    }

    public static final Module DESERIALIZE_IMMUTABLE_BUILDER_NESTED = new Module() {

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
                public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1,
                                                             AnnotatedMethod setter2) {
                    if (isImmutableBuilder(setter1.getDeclaringClass())) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("resolving setter conflict for Immutables Builder {} {}", setter1, setter2);
                        }
                        if (isImmutableBuilder(setter1.getRawParameterType(0))) {
                            return setter1;
                        }
                        if (isImmutableBuilder(setter2.getRawParameterType(0))) {
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

    public static final Module DESERIALIZE_API_BUILDINGBLOCK_MIGRATION = new Module() {

        @Override
        public String getModuleName() {
            return "DESERIALIZE_API_BUILDINGBLOCK_MIGRATION";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
            context.addDeserializationProblemHandler(new DeserializationProblemHandler() {
                @Override
                public JavaType handleMissingTypeId(DeserializationContext ctxt, JavaType baseType,
                                                    TypeIdResolver idResolver, String failureMsg) throws IOException {
                    if(!failureMsg.contains("'buildingBlock'")) {
                        return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
                    }

                    JsonParser p = ctxt.getParser();

                    JsonLocation currentLocation = p.getCurrentLocation();
                    byte[] sourceRef = (byte[]) currentLocation
                                                 .getSourceRef();
                    long line = currentLocation
                                       .getLineNr();
                    long column = currentLocation.getColumnNr();

                    JsonParser parser2 = p.getCodec()
                                         .getFactory()
                                         .createParser(sourceRef);
                    parser2.nextToken();
                    parser2.nextToken();
                    parser2.nextToken();


                    // but first, sanity check to ensure we have START_OBJECT or FIELD_NAME
                    JsonToken currentToken = parser2.nextToken();
                    if (currentToken == JsonToken.START_OBJECT) {
                        currentToken = parser2.nextToken();
                    } else if (/*t == JsonToken.START_ARRAY ||*/ currentToken != JsonToken.FIELD_NAME) {
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

                    long currentLine = parser2.getCurrentLocation()
                            .getLineNr();
                    long currentColumn = parser2.getCurrentLocation().getColumnNr();

                    String lastExtensionType = null;

                    while (currentLine < line || currentColumn < column) {

                        for (; currentToken != JsonToken.END_OBJECT; currentToken = parser2.nextToken()) {
                            if (currentToken == JsonToken.FIELD_NAME && parser2.getCurrentName()
                                                                    .equals("extensionType")) {
                                currentToken = parser2.nextToken();
                                lastExtensionType = parser2.getValueAsString();
                            }
                        }

                        currentLine = parser2.getCurrentLocation()
                                                  .getLineNr();
                        currentColumn = parser2.getCurrentLocation()
                                                  .getColumnNr();
                        currentToken = parser2.nextToken();
                    }

                    if (currentLine == line && currentColumn == column && Objects.nonNull(lastExtensionType)) {
                        return idResolver.typeFromId(ctxt, lastExtensionType);
                    }

                    return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
                }
            });
        }
    };

}
