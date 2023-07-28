/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.auth.app.User.UserData;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.Entity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.OptionalLong;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Entity(type = User.ENTITY_TYPE, data = UserData.class)
public class User extends AbstractPersistentEntity<User.UserData> {

  public static final String ENTITY_TYPE = "users";

  @AssistedInject
  public User(@Assisted UserData data) {
    super(data);
  }

  @Override
  public String getType() {
    return ENTITY_TYPE;
  }

  @Value.Immutable
  @Value.Modifiable
  @Value.Style(builder = "new")
  @JsonDeserialize(builder = ImmutableUserData.Builder.class)
  public interface UserData extends EntityData {

    abstract class Builder implements EntityDataBuilder<UserData> {}

    String getPassword();

    Role getRole();

    OptionalLong getPasswordExpiresAt();
  }
}
