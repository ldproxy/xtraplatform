package de.ii.xtraplatform.entity.api;

import com.google.common.collect.ObjectArrays;

import java.io.IOException;
import java.util.List;

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
    public List<String> getEntityIds() {
        return transformIds(entityRepository.getEntityIds());
    }

    @Override
    public boolean hasEntity(String id) {
        return entityRepository.hasEntity(transformId(id));
    }

    @Override
    public AbstractEntityData getEntityData(String id) {
        return entityRepository.getEntityData(transformId(id));
    }

    @Override
    public AbstractEntityData createEntity(AbstractEntityData data, String... path) throws IOException {
        return entityRepository.createEntity(transformData(data), transformPath(data.getId(), path));
    }

    @Override
    public AbstractEntityData replaceEntity(AbstractEntityData data) throws IOException {
        return entityRepository.replaceEntity(transformData(data));
    }

    @Override
    public AbstractEntityData updateEntity(AbstractEntityData partialData) throws IOException {
        return entityRepository.updateEntity(transformData(partialData));
    }

    @Override
    public void deleteEntity(String id) throws IOException {
        entityRepository.deleteEntity(transformId(id));
    }

    @Override
    public void addChangeListener(EntityRepositoryChangeListener listener) {
        entityRepository.addChangeListener(listener);
    }
}
