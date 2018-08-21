package de.ii.xtraplatform.entity.api;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface EntityRegistry {
    <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> clazz, String type);
    <T extends PersistentEntity> Optional<T> getEntity(Class<T> clazz, String type, String id);
}
