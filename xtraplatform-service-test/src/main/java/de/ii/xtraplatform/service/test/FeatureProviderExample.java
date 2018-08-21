package de.ii.xtraplatform.service.test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
