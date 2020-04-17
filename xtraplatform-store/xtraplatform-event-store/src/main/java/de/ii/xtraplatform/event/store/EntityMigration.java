package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entities.domain.EntityData;

import java.util.Map;

public interface EntityMigration<T extends EntityData, U extends EntityData> {

    long getSourceVersion();

    long getTargetVersion();

    EntityDataBuilder<T> getDataBuilder();

    U migrate(T entityData);

    Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, T entityData);
}
