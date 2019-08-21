package de.ii.xtraplatform.auth.internal;

import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.auth.api.UserAuthenticator;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.event.store.EntityDataStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class InternalUserAuthenticator implements UserAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalUserAuthenticator.class);

    private final EntityDataStore<de.ii.xtraplatform.auth.internal.User.UserData> entityRepository;

    public InternalUserAuthenticator(@Requires EntityDataStore<EntityData> entityRepository) {
        this.entityRepository = entityRepository.forType(de.ii.xtraplatform.auth.internal.User.UserData.class);//new EntityRepositoryForType(entityRepository, de.ii.xtraplatform.auth.internal.User.ENTITY_TYPE);
    }

    @Override
    public Optional<User> authenticate(String username, String password) {

        if (entityRepository.has(username)) {

            de.ii.xtraplatform.auth.internal.User.UserData entityData = entityRepository.get(username);

            if (PasswordHash.validatePassword(password, entityData.getPassword())) {
                LOGGER.debug("Authenticated {} {} {}", entityData.getId(), entityData.getRole(), PasswordHash.createHash(password));

                return Optional.of(ImmutableUser.builder()
                                                .name(entityData.getId())
                                                .role(entityData.getRole())
                                                .build());
            }
        }

        return Optional.empty();
    }
}
