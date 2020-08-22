package de.ii.xtraplatform.services.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableServiceDataCommon.Builder.class)
public interface ServiceDataCommon extends ServiceData {

    static abstract class Builder implements EntityDataBuilder<ServiceData> {
    }

}
