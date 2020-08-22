package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.MergeableKeyValueStore;

import java.util.Map;

public interface EntityDataDefaultsStore extends MergeableKeyValueStore<Map<String, Object>> {
    String EVENT_TYPE = "defaults";

    EntityDataBuilder<EntityData> getBuilder(Identifier identifier);
}
