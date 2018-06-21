package de.ii.xtraplatform.entity.api;

import java.util.Map;

/**
 * @author zahnen
 */
public interface EntityRepositoryChangeListener {

    void onEntityCreate(String id, AbstractEntityData data);

    void onEntityUpdate(String id, AbstractEntityData data);

    void onEntityDelete(String id);

}
