package de.ii.xtraplatform.auth.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.auth.app.User.UserData;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class UserFactory extends AbstractEntityFactory<UserData, User> implements EntityFactory {

  @Inject
  public UserFactory(UserFactoryAssisted userFactoryAssisted) {
    super(userFactoryAssisted);
  }

  @Override
  public String type() {
    return User.ENTITY_TYPE;
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return User.class;
  }

  @Override
  public EntityDataBuilder<UserData> dataBuilder() {
    return new ImmutableUserData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return UserData.class;
  }

  @AssistedFactory
  public interface UserFactoryAssisted extends FactoryAssisted<UserData, User> {
    @Override
    User create(UserData data);
  }
}