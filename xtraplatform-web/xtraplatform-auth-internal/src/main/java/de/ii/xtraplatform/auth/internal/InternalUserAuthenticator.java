package de.ii.xtraplatform.auth.internal;

import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.auth.api.UserAuthenticator;
import de.ii.xtraplatform.dropwizard.api.ConfigurationProvider;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@Provides
@Instantiate
public class InternalUserAuthenticator implements UserAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalUserAuthenticator.class);
    private static final de.ii.xtraplatform.auth.internal.User.UserData SUPER_ADMIN = new ImmutableUserData.Builder()
            .id("admin")
            .password(PasswordHash.createHash("admin"))
            .role(Role.SUPERADMIN)
            .build();

    private final boolean isAccessRestricted;
    private final EntityDataStore<de.ii.xtraplatform.auth.internal.User.UserData> userRepository;

    public InternalUserAuthenticator(@Requires ConfigurationProvider configurationProvider,
                                     @Requires EntityDataStore<EntityData> entityRepository) {
        this.isAccessRestricted = Optional.ofNullable(configurationProvider.getConfiguration().store)
                                          .map(storeConfiguration -> storeConfiguration.secured)
                                          .orElse(false);
        this.userRepository = entityRepository.forType(de.ii.xtraplatform.auth.internal.User.UserData.class);
    }

    @Override
    public Optional<User> authenticate(String username, String password) {

        if (userRepository.has(username)) {

            de.ii.xtraplatform.auth.internal.User.UserData userData = userRepository.get(username);

            if (PasswordHash.validatePassword(password, userData.getPassword())) {
                LOGGER.debug("Authenticated {} {} {}", userData.getId(), userData.getRole(), PasswordHash.createHash(password));

                return Optional.of(ImmutableUser.builder()
                                                .name(userData.getId())
                                                .role(userData.getRole())
                                                .build());
            }
        }

        //TODO: for ldproxy make admin password settable via env variable, get from bundlecontext
        // then save the user with that password below (or just validate and return for read-only store )


        // no user exists yet
        if (userRepository.ids()
                          .isEmpty()) {

            if (!isAccessRestricted) {
                return Optional.of(ImmutableUser.builder()
                                                .name(SUPER_ADMIN.getId())
                                                .role(SUPER_ADMIN.getRole())
                                                .build());
            }

            if (Objects.equals(username, SUPER_ADMIN.getId()) && PasswordHash.validatePassword(password, SUPER_ADMIN.getPassword())) {
                LOGGER.debug("Authenticated {} {} {}", SUPER_ADMIN.getId(), SUPER_ADMIN.getRole(), PasswordHash.createHash(password));

                try {
                    de.ii.xtraplatform.auth.internal.User.UserData firstUser = userRepository.put(SUPER_ADMIN.getId(), SUPER_ADMIN).get();

                    return Optional.of(ImmutableUser.builder()
                                                    .name(firstUser.getId())
                                                    .role(firstUser.getRole())
                                                    .forceChangePassword(true)
                                                    .build());

                } catch (InterruptedException | ExecutionException e) {
                    //ignore
                }
            }
        }

        return Optional.empty();
    }
}
