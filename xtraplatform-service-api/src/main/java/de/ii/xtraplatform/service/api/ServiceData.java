/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.AbstractEntityData;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
//@Value.Immutable
//@Value.Modifiable
//@Value.Style(deepImmutablesDetection = true)
//@JsonSerialize(as = ImmutableServiceData.class)
//@JsonDeserialize(as = ModifiableServiceData.class)
public abstract class ServiceData extends AbstractEntityData {

    public abstract String getServiceType();

    public abstract String getLabel();

    public abstract Optional<String> getDescription();

    @Value.Default
    public boolean getShouldStart() {
        return false;
    }

    public abstract List<Notification> getNotifications();

    @Value.Default
    public boolean getSecured() {
        return false;
    }

    @JsonIgnore
    @Value.Derived
    public boolean isSecured() {
        return getSecured();
    }
}
