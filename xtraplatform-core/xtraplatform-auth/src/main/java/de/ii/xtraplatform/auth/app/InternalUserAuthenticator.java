package de.ii.xtraplatform.auth.app;

import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthenticator;
import de.ii.xtraplatform.dropwizard.domain.ConfigurationProvider;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
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
    private static final de.ii.xtraplatform.auth.app.User.UserData SUPER_ADMIN = new ImmutableUserData.Builder()
            .id("admin")
            .password(PasswordHash.createHash("admin"))
            .role(Role.SUPERADMIN)
            .build();

    private final boolean isAccessRestricted;
    private final EntityDataStore<de.ii.xtraplatform.auth.app.User.UserData> userRepository;

    public InternalUserAuthenticator(@Requires ConfigurationProvider configurationProvider,
                                     @Requires EntityDataStore<EntityData> entityRepository) {
        this.isAccessRestricted = Optional.ofNullable(configurationProvider.getConfiguration().store)
                                          .map(storeConfiguration -> storeConfiguration.secured)
                                          .orElse(false);
        this.userRepository = entityRepository.forType(de.ii.xtraplatform.auth.app.User.UserData.class);
    }

    @Override
    public Optional<User> authenticate(String username, String password) {

        if (userRepository.has(username)) {

            de.ii.xtraplatform.auth.app.User.UserData userData = userRepository.get(username);

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
                    de.ii.xtraplatform.auth.app.User.UserData firstUser = userRepository.put(SUPER_ADMIN.getId(), SUPER_ADMIN).get();

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
