package de.ii.xtraplatform.event.store;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityDataDefaultsStoreImpl extends AbstractMergeableKeyValueStore<EntityDataBuilder<EntityData>> implements EntityDataDefaultsStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataDefaultsStoreImpl.class);

    private final EntityFactory entityFactory;
    private final ValueEncodingJackson<EntityDataBuilder<EntityData>> valueEncoding;
    private final EventSourcing<EntityDataBuilder<EntityData>> eventSourcing;

    protected EntityDataDefaultsStoreImpl(@Requires EventStore eventStore, @Requires Jackson jackson,
                                          @Requires EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
        this.valueEncoding = new ValueEncodingJackson<>(jackson);
        this.eventSourcing = new EventSourcing<>(eventStore, ImmutableList.of(EntityDataDefaultsStore.EVENT_TYPE), valueEncoding, this::onStart, Optional.of(this::processEvent));

        valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
        valueEncoding.addDecoderMiddleware(new ValueDecoderBase<>(this::getBuilder, eventSourcing));
    }

    //TODO: onEmit middleware
    private List<MutationEvent> processEvent(MutationEvent event) {

        EntityDataDefaultsPath defaultsPath = EntityDataDefaultsPath.from(event.identifier());

        List<List<String>> subTypes = entityFactory.getSubTypes(defaultsPath.getEntityType(), defaultsPath.getEntitySubtype());

        LOGGER.debug("Applying to subtypes as well: {}", subTypes);

        List<Identifier> cacheKeys = getCacheKeys(defaultsPath, subTypes);

        LOGGER.debug("Applying to subtypes as well 2: {}", cacheKeys);

        return cacheKeys.stream()
                        .map(cacheKey -> {
                            ImmutableMutationEvent.Builder builder = ImmutableMutationEvent.builder()
                                                                                           .from(event)
                                                                                           .identifier(cacheKey);
                            if (!defaultsPath.getKeyPath()
                                             .isEmpty()) {
                                try {
                                    byte[] nestedPayload = valueEncoding.nestPayload(event.payload(), ValueEncoding.FORMAT.fromString(event.format()), defaultsPath.getKeyPath());
                                    builder.payload(nestedPayload);
                                } catch (IOException e) {
                                    LOGGER.error("Error:", e);
                                }
                            }

                            return builder.build();
                        })
                        .collect(Collectors.toList());
    }

    private List<Identifier> getCacheKeys(EntityDataDefaultsPath defaultsPath, List<List<String>> subTypes) {

        return ImmutableList.<Identifier>builder()
                .add(ImmutableIdentifier.builder()
                                        .addPath(defaultsPath.getEntityType())
                                        .addAllPath(defaultsPath.getEntitySubtype())
                                        .id(EntityDataDefaultsStore.EVENT_TYPE)
                                        .build())
                .addAll(subTypes.stream()
                                .map(subType -> ImmutableIdentifier.builder()
                                                                   .addPath(defaultsPath.getEntityType())
                                                                   .addAllPath(subType)
                                                                   .id(EntityDataDefaultsStore.EVENT_TYPE)
                                                                   .build())
                                .collect(Collectors.toList()))
                .build();
    }

    private EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {

        if (eventSourcing.isInCache(identifier)) {
            return eventSourcing.getFromCache(identifier);
        }

        EntityDataDefaultsPath defaultsPath = EntityDataDefaultsPath.from(identifier);

        Optional<String> subtype = entityFactory.getTypeAsString(defaultsPath.getEntitySubtype());

        return entityFactory.getDataBuilder(defaultsPath.getEntityType(), subtype);
    }

    @Override
    protected ValueEncoding<EntityDataBuilder<EntityData>> getValueEncoding() {
        return valueEncoding;
    }

    @Override
    protected EventSourcing<EntityDataBuilder<EntityData>> getEventSourcing() {
        return eventSourcing;
    }

    @Override
    public <U extends EntityDataBuilder<EntityData>> MergeableKeyValueStore<U> forType(Class<U> type) {
        return null;
    }

    @Override
    protected CompletableFuture<Void> onStart() {

        identifiers().forEach(identifier -> {
            EntityDataBuilder<EntityData> builder = get(identifier);

            LOGGER.debug("Loaded defaults: {}", identifier);

            /*try {
                builder.build();
            } catch (Throwable e) {
                LOGGER.debug("Error: {}", e.getMessage());
            }*/

        });

        return super.onStart();
    }
}
