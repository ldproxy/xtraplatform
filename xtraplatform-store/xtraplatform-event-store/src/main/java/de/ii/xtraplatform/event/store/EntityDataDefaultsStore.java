package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

public interface EntityDataDefaultsStore extends MergeableKeyValueStore<EntityDataBuilder<EntityData>> {
    String EVENT_TYPE = "defaults";
}
