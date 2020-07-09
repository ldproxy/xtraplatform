package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

import java.util.Map;

public interface EntityDataDefaults<T extends EntityData> {

    T getDefaults();

    EntityDataBuilder<T> getBuilderWithDefaults();

    Map<String, String> getAliases();
}
