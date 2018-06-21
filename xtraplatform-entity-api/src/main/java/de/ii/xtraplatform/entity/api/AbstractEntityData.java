package de.ii.xtraplatform.entity.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xsf.core.api.Resource;

/**
 * @author zahnen
 */
// TODO: move
public abstract class AbstractEntityData implements EntityData, /*TODO remove*/Resource {

    public abstract String getId();

    public abstract long getCreatedAt();

    public abstract long getLastModified();

    @JsonIgnore
    @Override
    public String getResourceId() {
        return getId();
    }

    @Override
    public void setResourceId(String id) {

    }
}
