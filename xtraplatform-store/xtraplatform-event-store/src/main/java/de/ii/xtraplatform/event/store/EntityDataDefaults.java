package de.ii.xtraplatform.event.store;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;

import java.util.Map;

public interface EntityDataDefaults<T extends EntityData> {

    int getSortPriority();

    EntityDataBuilder<T> getBuilderWithDefaults();

    default Map<String, String> getAliases() {
        return ImmutableMap.of();
    }
}
