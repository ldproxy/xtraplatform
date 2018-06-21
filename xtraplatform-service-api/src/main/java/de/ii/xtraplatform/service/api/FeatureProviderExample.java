package de.ii.xtraplatform.service.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
//@JsonSerialize(as = ImmutableFeatureProviderExample.class)
@JsonDeserialize(as = ModifiableFeatureProviderExample.class)
public abstract class FeatureProviderExample {

    @Value.Default
    public boolean getUseBasicAuth() {
        return false;
    }

    public abstract Optional<String> getBasicAuthCredentials();
}
