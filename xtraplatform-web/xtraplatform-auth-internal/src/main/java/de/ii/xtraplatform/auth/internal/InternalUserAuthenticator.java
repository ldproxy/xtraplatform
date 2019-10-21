package de.ii.xtraplatform.auth.internal;

import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.auth.api.UserAuthenticator;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Provides
@Instantiate
public class InternalUserAuthenticator implements UserAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalUserAuthenticator.class);

    private final EntityDataStore<de.ii.xtraplatform.auth.internal.User.UserData> userRepository;

    public InternalUserAuthenticator(@Requires EntityDataStore<EntityData> entityRepository) {
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
        // then save the user with that password below


        // no user exists yet
        if (userRepository.ids().isEmpty()) {
            de.ii.xtraplatform.auth.internal.User.UserData firstUser = new ImmutableUserData.Builder()
                    .id("admin")
                    .password(PasswordHash.createHash("admin"))
                    .role(Role.SUPERADMIN)
                    .build();

            return Optional.of(ImmutableUser.builder()
                                           .name(firstUser.getId())
                                           .role(firstUser.getRole())
                                           .build());

            /*try {
                de.ii.xtraplatform.auth.internal.User.UserData admin = userRepository.put("admin", firstUser).get();

                return Optional.of(ImmutableUser.builder()
                                                .name(admin.getId())
                                                .role(admin.getRole())
                                                .build());

            } catch (InterruptedException | ExecutionException e) {
                //ignore
            }*/
        }

        return Optional.empty();
    }
}
