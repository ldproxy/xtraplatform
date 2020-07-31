package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

import java.util.concurrent.CompletableFuture;

public interface EntityDataDefaultsStore extends MergeableKeyValueStore<EntityDataBuilder<EntityData>> {
    String EVENT_TYPE = "defaults";

    EntityDataBuilder<EntityData> getBuilder(Identifier identifier);
}
