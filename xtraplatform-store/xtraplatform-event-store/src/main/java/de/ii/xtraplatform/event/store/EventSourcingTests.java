package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*TODO: existing nodeRevision should kept or expanded to per type or per instance revisions
  TODO: revisions can be implemented as G-Counter CRDT (does that help??? still need transactions...)
  TODO: revision is then used for optimistic locking on event store
  TODO: how to delete???
 */


public class EventSourcingTests {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventSourcingTests.class);

    /*public static void replay() {
        ObjectMapper objectMapper = new ObjectMapper()
                //TODO: needed even if setters are never used
                .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                .setDefaultMergeable(true);

        try {

            ImmutableValueObject.Builder builder = objectMapper.readerFor(ImmutableValueObject.Builder.class)
                                                               .readValue("{\"id\": 1, \"name\": \"foo\"}");

            LOGGER.debug("CRDT: {}", builder.build());

            objectMapper.readerForUpdating(builder)
                        .readValue("{\"fields\": [\"bar\"]}");

            LOGGER.debug("CRDT: {}", builder.build());

            objectMapper.readerForUpdating(builder)
                        .readValue("{\"fields\": [\"bla\", \"blub\"]}");

            LOGGER.debug("CRDT: {}", builder.build());

            objectMapper.readerForUpdating(builder)
                        .readValue("{\"nested\": {\"id\": 1, \"name\": \"abc\"}}");

            LOGGER.debug("CRDT: {}", builder.build());

            objectMapper.readerForUpdating(builder)
                        .readValue("{\"nested\": {\"name\": \"xyz\"}}");

            LOGGER.debug("CRDT: {}", builder.build());


        } catch (IOException e) {
            LOGGER.debug("", e);
        }
    }*/

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

}
