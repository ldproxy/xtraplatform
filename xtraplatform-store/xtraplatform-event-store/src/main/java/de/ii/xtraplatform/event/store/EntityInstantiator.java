package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

public interface EntityInstantiator<T extends EntityData> {

    void createInstance(String type, String id, T data);

    void updateInstance(String type, String id, T data);

    void deleteInstance(String id);
}
