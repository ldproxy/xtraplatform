package de.ii.xtraplatform.service.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableServiceDataCommon.Builder.class)
public interface ServiceDataCommon extends ServiceData {

    static abstract class Builder implements EntityDataBuilder<ServiceData> {
    }

}
