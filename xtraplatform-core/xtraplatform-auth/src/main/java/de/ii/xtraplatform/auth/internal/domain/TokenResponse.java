package de.ii.xtraplatform.auth.internal.domain;

import org.immutables.value.Value;

@Value.Immutable
public interface TokenResponse {

    String getAccess_token();

    @Value.Default
    default int getExpires_in() {return 60;}
}
