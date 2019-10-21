package de.ii.xtraplatform.auth.internal.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Value.Immutable
public interface TokenResponse {

    String getAccess_token();

    @Value.Default
    default int getExpires_in() {return 60;}
}
