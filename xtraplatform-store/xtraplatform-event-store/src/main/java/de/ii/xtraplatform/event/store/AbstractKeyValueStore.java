package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entities.domain.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class AbstractKeyValueStore<T> implements EventStoreSubscriber, KeyValueStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKeyValueStore.class);

    private final String eventType;
    final EventSourcing<T> eventSourcing;

    protected AbstractKeyValueStore(EventStore eventStore, String eventType) {
        this.eventType = eventType;
        this.eventSourcing = new EventSourcing<>(eventStore, eventType, this::serialize, this::deserialize, this::getDefaultFormat);
        eventStore.subscribe(this);
    }

    protected abstract byte[] serialize(T value);

    protected abstract T deserialize(Identifier identifier, byte[] payload, String format);

    protected abstract String getDefaultFormat();

    protected CompletableFuture<Void> onStart() {
        return CompletableFuture.completedFuture(null);
    }

    protected CompletableFuture<PersistentEntity> onCreate(Identifier identifier, T entityData) {
        return null;
    }

    protected void onUpdate(Identifier identifier, T entityData) {}

    protected void onDelete(Identifier identifier) {}

    protected void onFailure(Identifier identifier, Throwable throwable) {}

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public void onEmit(Event event) {
        //TODO: when isReplay switches, notify EntityInstantiator
        if (event instanceof MutationEvent) {
            eventSourcing.onEmit((MutationEvent) event);

        } else if (event instanceof StateChangeEvent) {
            switch (((StateChangeEvent) event).state()) {
                case REPLAYING:
                    LOGGER.debug("Replaying events for {}", getEventType());
                    break;
                case LISTENING:
                    onStart().thenRun(() -> LOGGER.debug("Listening for events for {}", getEventType()));
                    break;
            }
        }
    }

    @Override
    public List<String> ids(String... path) {
        return identifiers(path)
                            .stream()
                            .map(Identifier::id)
                            .collect(Collectors.toList());
    }

    @Override
    public boolean has(String id, String... path) {
        return Objects.nonNull(id) && has(Identifier.from(id, path));
    }

    @Override
    public T get(String id, String... path) {
        return get(Identifier.from(id, path));
    }

    @Override
    public CompletableFuture<T> put(String id, T value, String... path) {
        return put(Identifier.from(id, path), value);
    }

    @Override
    public CompletableFuture<Boolean> delete(String id, String... path) {
        return drop(Identifier.from(id, path));
    }

    protected List<Identifier> identifiers(String... path) {
        return eventSourcing.getIdentifiers(path);
    }

    protected boolean has(Identifier identifier) {
        return eventSourcing.isInCache(identifier);
    }

    protected T get(Identifier identifier) {
        return eventSourcing.getFromCache(identifier);
    }

    protected CompletableFuture<T> put(Identifier identifier, T value) {
        boolean exists = has(identifier);

        return eventSourcing.pushMutationEvent(identifier, value)
                            .whenComplete((entityData, throwable) -> {
                                if (Objects.nonNull(throwable)) {
                                    onFailure(identifier, throwable);
                                } else if (Objects.nonNull(entityData)) {
                                    if (exists) onUpdate(identifier, entityData);
                                    else onCreate(identifier, entityData);
                                }
                            });
    }

    protected CompletableFuture<T> putWithoutTrigger(Identifier identifier, T value) {
        return eventSourcing.pushMutationEvent(identifier, value);
    }

    protected CompletableFuture<Boolean> drop(Identifier identifier) {
        return eventSourcing.pushMutationEvent(identifier, null)
                            .whenComplete((entityData, throwable) -> {
                                if (Objects.nonNull(throwable)) {
                                    onFailure(identifier, throwable);
                                } else if (Objects.isNull(entityData)) {
                                    onDelete(identifier);
                                }
                            })
                            .thenApply(Objects::isNull);
    }

    protected CompletableFuture<Boolean> dropWithoutTrigger(Identifier identifier) {
        return eventSourcing.pushMutationEvent(identifier, null)
                            .thenApply(Objects::isNull);
    }
}
