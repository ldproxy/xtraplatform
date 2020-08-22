package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;

import java.util.Map;

public interface EntityMigration<T extends EntityData, U extends EntityData> {

    long getSourceVersion();

    long getTargetVersion();

    EntityDataBuilder<T> getDataBuilder();

    U migrate(T entityData);

    Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, T entityData);
}
