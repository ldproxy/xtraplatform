/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.auth.domain.Role
import de.ii.xtraplatform.dropwizard.domain.ConfigurationProvider
import de.ii.xtraplatform.auth.domain.User
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration
import de.ii.xtraplatform.store.domain.Identifier
import de.ii.xtraplatform.store.domain.ValueEncoding
import de.ii.xtraplatform.store.domain.entities.EntityData
import de.ii.xtraplatform.store.domain.entities.EntityDataStore
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class InternalUserAuthenticatorSpec extends Specification {

    @Shared InternalUserAuthenticator internalUserAuthenticator

    def setupSpec() {
        internalUserAuthenticator = getinternalUserAuthenticatorMock()
    }


    def 'Test user authentication'() {
        given:
        String username = "testUser"
        String password = "userPassword"

        when:
        Optional<User> user = internalUserAuthenticator.authenticate(username, password)

        then:
        user.isPresent()
        user.get().getRole() == Role.USER
        user.get().getName() == username
    }

    def 'Test admin authentication'() {
        given:
        String username = "testAdmin"
        String password = "adminPassword"

        when:
        Optional<User> user = internalUserAuthenticator.authenticate(username, password)

        then:
        user.isPresent()
        user.get().getRole() == Role.ADMIN
        user.get().getName() == username
    }

    def 'Test authentication on different inputs'() {
        when:
        Optional<User> user = internalUserAuthenticator.authenticate(username, password)

        then:
        user.isEmpty()

        where:
        username            | password
        null                | null              // username and password are null
        ""                  | ""                // empty username and password
        "foobar"            | "userPassword"    // non-existent user
        "testUser"          | ""                // existing user, empty password
        "testUser"          | "password"        // existing user, wrong password
    }

    InternalUserAuthenticator getinternalUserAuthenticatorMock() {
        ConfigurationProvider configurationProvider = new ConfigurationProvider() {
            @Override
            XtraPlatformConfiguration getConfiguration() {
                return new XtraPlatformConfiguration()
            }
        }
        EntityDataStore<EntityData> entityDataStore = new EntityDataStoreTest()
        return new InternalUserAuthenticator(configurationProvider, entityDataStore)
    }

    class EntityDataStoreTest implements EntityDataStore<EntityData>{

        @Override
        CompletableFuture<EntityData> patch(String id, Map<String, Object> partialData, String... path) {
            return null
        }

        @Override
        ValueEncoding<EntityData> getValueEncoding() {
            return null
        }

        @Override
        <U extends EntityData> EntityDataStore<U> forType(Class<U> type) {
            if (type == de.ii.xtraplatform.auth.app.User.UserData.class) {
                return this as EntityDataStore<U>
            }
            return null
        }

        @Override
        Map<String, Object> asMap(Identifier identifier, EntityData entityData) throws IOException {
            return null
        }

        @Override
        EntityData fromMap(Identifier identifier, Map<String, Object> entityData) throws IOException {
            return null 
        }

        @Override
        EntityData fromBytes(Identifier identifier, byte[] entityData) throws IOException {
            return null
        }

        @Override
        List<String> ids(String... path) {
            return ImmutableList.of("testAdmin", "testUser")
        }

        @Override
        boolean has(String id, String... path) {
            if (id == "testUser" || id == "testAdmin") {
                return true
            }
            return false
        }

        @Override
        EntityData get(String id, String... path) {
            de.ii.xtraplatform.auth.app.User.UserData.Builder userData = new ImmutableUserData.Builder().id(id)
            if (id == "testUser") {
                userData.role(Role.USER)
                        .password(PasswordHash.createHash("userPassword"))
            } else if (id == "testAdmin") {
                userData.role(Role.ADMIN)
                        .password(PasswordHash.createHash("adminPassword"))
            }
            return userData.build()
        }

        @Override
        CompletableFuture<EntityData> put(String id, EntityData value, String... path) {
            return null
        }

        @Override
        CompletableFuture<Boolean> delete(String id, String... path) {
            return null
        }

        @Override
        List<Identifier> identifiers(String... path) {
            return null
        }

        @Override
        boolean has(Identifier identifier) {
            return false
        }

        @Override
        EntityData get(Identifier identifier) {
            return null
        }

        @Override
        CompletableFuture<EntityData> put(Identifier identifier, EntityData value) {
            return null
        }
    }

}
