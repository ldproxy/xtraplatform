/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public abstract class EntityRepositoryWrapper implements EntityRepository {

    private final EntityRepository entityRepository;

    public EntityRepositoryWrapper(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    protected String transformId(String id) {
        return id;
    }

    protected List<String> transformIds(List<String> ids) {
        return ids;
    }

    protected AbstractEntityData transformData(AbstractEntityData data) {
        return data;
    }

    protected String[] transformPath(String id, String... path) {
        return path;
    }

    @Override
    public List<String> getEntityTypes() {
        return entityRepository.getEntityTypes();
    }

    @Override
    public List<String> getEntityIds(String... path) {
        return entityRepository.getEntityIds(transformPath(null, path));
    }

    @Override
    public boolean hasEntity(String id, String... path) {
        return entityRepository.hasEntity(id, transformPath(id, path));
    }

    @Override
    public AbstractEntityData getEntityData(String id, String... path) {
        return entityRepository.getEntityData(id, transformPath(id, path));
    }

    @Override
    public AbstractEntityData createEntity(AbstractEntityData data, String... path) throws IOException {
        return entityRepository.createEntity(transformData(data), transformPath(data.getId(), path));
    }

    @Override
    public AbstractEntityData generateEntity(Map<String, Object> data, String... path) throws IOException {
        return entityRepository.generateEntity(data, transformPath((String) data.get("id"), path));
    }

    @Override
    public AbstractEntityData replaceEntity(AbstractEntityData data) throws IOException {
        return entityRepository.replaceEntity(transformData(data));
    }

    @Override
    public AbstractEntityData updateEntity(AbstractEntityData partialData, String... path) throws IOException {
        return entityRepository.updateEntity(transformData(partialData), transformPath(partialData.getId(), path));
    }

    @Override
    public AbstractEntityData updateEntity(String id, String partialData, String... path) throws IOException {
        return entityRepository.updateEntity(id, partialData, transformPath(id, path));
    }

    @Override
    public void deleteEntity(String id) throws IOException {
        entityRepository.deleteEntity(transformId(id));
    }

    @Override
    public void addChangeListener(EntityRepositoryChangeListener listener) {
        entityRepository.addChangeListener(listener);
    }

    @Override
    public void addEntityType(String entityType, String dataType) {
        entityRepository.addEntityType(entityType, dataType);
    }
}
