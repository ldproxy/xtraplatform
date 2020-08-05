package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_API_BUILDINGBLOCK_MIGRATION;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER;

//TODO: make default format and supported formats configurable
public class ValueEncodingJackson<T> implements ValueEncoding<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueEncodingJackson.class);

    private static final byte[] JSON_NULL = "null".getBytes();
    private static final byte[] YAML_NULL = "--- null\n".getBytes();
    private static final Pattern JSON_EMPTY = Pattern.compile("(\\s)*");
    private static final Pattern YAML_EMPTY = Pattern.compile("---(\\s)*");

    private static final FORMAT DEFAULT_FORMAT = FORMAT.YML;
    private static final FORMAT DESER_FORMAT_LEGACY = FORMAT.JSON; // old configuration files without file extension are JSON

    private final Map<FORMAT, ObjectMapper> mappers; // TODO: use smile/ion mapper for distributed store
    private final List<ValueDecoderMiddleware<byte[]>> decoderPreProcessor;
    private final List<ValueDecoderMiddleware<T>> decoderMiddleware;

    ValueEncodingJackson(Jackson jackson) {

        ObjectMapper jsonMapper = jackson.getDefaultObjectMapper()
                                         .copy()
                                         .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                                         .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
                                         .registerModule(DESERIALIZE_API_BUILDINGBLOCK_MIGRATION)
                                         .setDefaultMergeable(true);

        ObjectMapper yamlMapper = jackson.getNewObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                                                                              .disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID)
                                                                              .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                                         .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                                         .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
                                         .registerModule(DESERIALIZE_API_BUILDINGBLOCK_MIGRATION)
                                         .setDefaultMergeable(true)
                                         .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        this.mappers = ImmutableMap.of(
                FORMAT.JSON, jsonMapper,
                FORMAT.YML, yamlMapper,
                FORMAT.YAML, yamlMapper
        );

        this.decoderMiddleware = new ArrayList<>();
        this.decoderPreProcessor = new ArrayList<>();
    }

    public void addDecoderPreProcessor(ValueDecoderMiddleware<byte[]> preProcessor) {
        this.decoderPreProcessor.add(preProcessor);
    }

    public void addDecoderMiddleware(ValueDecoderMiddleware<T> middleware) {
        this.decoderMiddleware.add(middleware);
    }

    @Override
    public final FORMAT getDefaultFormat() {
        return DEFAULT_FORMAT;
    }

    //TODO: default serialization format should depend on EventStore implementation
    @Override
    public final byte[] serialize(T data) {
        try {
            return getDefaultMapper().writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    @Override
    public byte[] serialize(Map<String, Object> data) {
        try {
            return getDefaultMapper().writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    @Override
    public final T deserialize(Identifier identifier, byte[] payload, FORMAT format) {
        // "null" as payload means delete
        if (isNull(payload)) {
            return null;
        }

        byte[] rawData = payload;
        FORMAT payloadFormat = Objects.equals(format, FORMAT.UNKNOWN) ? DESER_FORMAT_LEGACY : format;
        ObjectMapper objectMapper = getMapper(payloadFormat);
        T data = null;

        try {
            for (ValueDecoderMiddleware<byte[]> preProcessor : decoderPreProcessor) {
                rawData = preProcessor.process(identifier, rawData, objectMapper, null);
            }
        } catch (Throwable e) {
            LOGGER.error("Deserialization error: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", e);
            }
        }

        try {
            for (ValueDecoderMiddleware<T> middleware : decoderMiddleware) {
                data = middleware.process(identifier, rawData, objectMapper, data);
            }

        } catch (Throwable e) {
            try {
                Optional<ValueDecoderMiddleware<T>> recovery = decoderMiddleware.stream()
                                                                                .filter(ValueDecoderMiddleware::canRecover)
                                                                                .findFirst();
                if (recovery.isPresent()) {
                    data = recovery.get()
                                   .recover(identifier, rawData, objectMapper);
                }
            } catch (Throwable e2) {
                LOGGER.error("Deserialization error: {}", e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace:", e);
                }
            }
        }

        return data;
    }

    @Override
    public byte[] nestPayload(byte[] payload, String formatString, List<String> nestingPath, Optional<EntityDataDefaults.KeyPathAlias> keyPathAlias) throws IOException {
        if (nestingPath.isEmpty()) {
            return payload;
        }

        FORMAT format;
        try {
            format = FORMAT.fromString(formatString);
        } catch (Throwable e) {
            //LOGGER.error("Could not deserialize, format '{}' unknown.", formatString);
            return payload;
        }


        //TODO: .metadata.yml.swp leads to invisible error, should be ignored either silently or with log message

        ObjectMapper mapper = getMapper(format);

        Map<String, Object> data = mapper.readValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {
        });

        for (int i = nestingPath.size() - 1; i >= 0; i--) {
            if (i == nestingPath.size() - 1 && keyPathAlias.isPresent()) {
                data = keyPathAlias.get().wrapMap(data);
                continue;
            }

            String key = nestingPath.get(i);
            data = ImmutableMap.of(key, data);
        }
        return mapper.writeValueAsBytes(data);
    }

    final ObjectMapper getDefaultMapper() {
        return getMapper(DEFAULT_FORMAT);
    }

    final ObjectMapper getMapper(FORMAT format) {
        return mappers.get(format);
    }

    final boolean isNull(byte[] payload) {
        return Arrays.equals(payload, JSON_NULL) || Arrays.equals(payload, YAML_NULL);
    }

    final boolean isEmpty(byte[] payload) {
        String payloadString = new String(payload, StandardCharsets.UTF_8);
        return JSON_EMPTY.matcher(payloadString).matches() || YAML_EMPTY.matcher(payloadString).matches();
    }
}
