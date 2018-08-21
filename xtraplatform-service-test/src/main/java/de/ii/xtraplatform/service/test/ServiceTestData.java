package de.ii.xtraplatform.service.test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.service.api.ServiceData;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableServiceTestData.class)
public abstract class ServiceTestData extends ServiceData {

    public abstract FeatureProviderExample getFeatureProviderData();
}
