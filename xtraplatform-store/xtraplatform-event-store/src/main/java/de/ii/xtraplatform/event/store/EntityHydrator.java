package de.ii.xtraplatform.event.store;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;

import java.util.Map;

public interface EntityHydrator<T extends EntityData> {

    default Map<String, Object> getInstanceConfiguration(T data) {
        return ImmutableMap.of("data", data);
    }
}
