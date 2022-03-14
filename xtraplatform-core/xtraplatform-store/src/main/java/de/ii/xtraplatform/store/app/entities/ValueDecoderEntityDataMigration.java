/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiConsumer;

public class ValueDecoderEntityDataMigration implements ValueDecoderMiddleware<EntityData> {

  private final EventSourcing<EntityData> eventSourcing;
  // private final EntityFactory3 entityFactory;
  private final BiConsumer<Identifier, EntityData> addAdditionalEvent;

  public ValueDecoderEntityDataMigration(
      EventSourcing<EntityData> eventSourcing,
      // EntityFactory3 entityFactory,
      BiConsumer<Identifier, EntityData> addAdditionalEvent) {
    this.eventSourcing = eventSourcing;
    // this.entityFactory = entityFactory;
    this.addAdditionalEvent = addAdditionalEvent;
  }

  @Override
  public EntityData process(
      Identifier identifier,
      byte[] payload,
      ObjectMapper objectMapper,
      EntityData entityData,
      boolean ignoreCache)
      throws IOException {
    if (entityData.getEntityStorageVersion() < entityData.getEntitySchemaVersion()) {
      migrateSchema(
          identifier,
          payload,
          objectMapper,
          entityData.getEntityStorageVersion(),
          entityData.getEntitySubType(),
          OptionalLong.of(entityData.getEntitySchemaVersion()));

      return null;
    }

    return entityData;
  }

  @Override
  public boolean canRecover() {
    return true;
  }

  // TODO: entitySubType
  @Override
  public EntityData recover(Identifier identifier, byte[] payload, ObjectMapper objectMapper)
      throws IOException {
    TypeReference<LinkedHashMap<String, Object>> typeRef =
        new TypeReference<LinkedHashMap<String, Object>>() {};
    Map<String, Object> map = objectMapper.readValue(payload, typeRef);

    if (!map.containsKey("id")) {
      throw new IllegalArgumentException("not a valid entity, no id found");
    }

    long storageVersion = ((Number) map.getOrDefault("entityStorageVersion", 1)).longValue();

    migrateSchema(
        identifier, payload, objectMapper, storageVersion, Optional.empty(), OptionalLong.empty());

    return null;
  }

  private void migrateSchema(
      Identifier identifier,
      byte[] payload,
      ObjectMapper objectMapper,
      long storageVersion,
      Optional<String> entitySubType,
      OptionalLong targetVersion)
      throws IOException {
    /*EntityDataBuilder<EntityData> builderOld =
        entityFactory.getDataBuilders(identifier.path().get(0), storageVersion, entitySubType);
    ValueDecoderWithBuilder<EntityData> valueDecoderWithBuilder =
        new ValueDecoderWithBuilder<>(identifier1 -> builderOld, eventSourcing);
    EntityData entityDataOld =
        valueDecoderWithBuilder.process(identifier, payload, objectMapper, null, false);

    String entityType = identifier.path().get(0);

    Map<Identifier, EntityData> entityDataNew =
        entityFactory.migrateSchema(
            identifier, entityType, entityDataOld, entitySubType, targetVersion);

    entityDataNew.forEach(addAdditionalEvent);*/
  }
}
