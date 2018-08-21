package de.ii.xtraplatform.service.api;

import de.ii.xtraplatform.entity.api.PersistentEntity;

/**
 * @author zahnen
 */
public interface Service extends PersistentEntity {

    String ENTITY_TYPE = "services";

    @Override
    default String getType() {
        return ENTITY_TYPE;
    }

    default String getServiceType() {
        return getData().getServiceType();
    }

    @Override
    default ServiceData getData() {
        return getData();
    }
}
