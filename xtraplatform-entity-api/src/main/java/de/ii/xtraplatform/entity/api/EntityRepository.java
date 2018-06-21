package de.ii.xtraplatform.entity.api;

import java.io.IOException;
import java.util.List;

/**
 * id maybe TYPE/ORG/ID, in that case a multitenant middleware would handle splitting into path and id
 *
 * @author zahnen
 */
public interface EntityRepository {

    String ID_SEPARATOR = "/";


    List<String> getEntityIds();

    boolean hasEntity(String id);

    AbstractEntityData getEntityData(String id);

    AbstractEntityData createEntity(AbstractEntityData data, String... path) throws IOException;

    AbstractEntityData replaceEntity(AbstractEntityData data) throws IOException;

    AbstractEntityData updateEntity(AbstractEntityData partialData) throws IOException;

    void deleteEntity(String id) throws IOException;

    void addChangeListener(EntityRepositoryChangeListener listener);
}