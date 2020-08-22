package de.ii.xtraplatform.store.domain.entities;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.store.domain.KeyPathAlias;

import java.util.Map;

public interface EntityDataDefaults<T extends EntityData> {

    int getSortPriority();

    EntityDataBuilder<T> getBuilderWithDefaults();

    default Map<String, KeyPathAlias> getAliases() {
        return ImmutableMap.of();
    }
}
