package de.ii.xtraplatform.entities.domain;

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
