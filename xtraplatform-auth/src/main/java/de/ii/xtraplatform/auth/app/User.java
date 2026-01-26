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
import de.ii.xtraplatform.entities.domain.AbstractPersistentEntity;
import de.ii.xtraplatform.entities.domain.Entity;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import java.util.OptionalLong;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
// TODO: make value
@Entity(type = User.ENTITY_TYPE, data = UserData.class)
public class User extends AbstractPersistentEntity<User.UserData> {

  public static final String ENTITY_TYPE = "users";

  @AssistedInject
  public User(@Assisted UserData data) {
    super(data, null);
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

    abstract class Builder implements EntityDataBuilder<UserData> {
      public abstract Builder id(String id);

      public abstract Builder password(String id);

      public abstract Builder role(Role role);

      @Override
      public ImmutableUserData.Builder fillRequiredFieldsWithPlaceholders() {
        return (ImmutableUserData.Builder)
            this.id("__DEFAULT__").password("__DEFAULT__").role(Role.NONE);
      }
    }

    String getPassword();

    Role getRole();

    OptionalLong getPasswordExpiresAt();
  }
}
