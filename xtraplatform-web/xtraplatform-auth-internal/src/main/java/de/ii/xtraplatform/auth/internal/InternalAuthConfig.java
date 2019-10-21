package de.ii.xtraplatform.auth.internal;

import com.google.common.base.Strings;
import de.ii.xtraplatform.auth.api.AuthConfig;
import de.ii.xtraplatform.cfgstore.api.BundleConfigDefault;
import de.ii.xtraplatform.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xtraplatform.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;

import static de.ii.xtraplatform.auth.api.AuthConfig.JWT_SIGNING_KEY;
import static de.ii.xtraplatform.auth.api.AuthConfig.USER_NAME_KEY;
import static de.ii.xtraplatform.auth.api.AuthConfig.USER_ROLE_KEY;

@Component
@Provides(specifications = {InternalAuthConfig.class, AuthConfig.class})
@Instantiate
@LocalBundleConfig(category = "Security", properties = {
        @ConfigPropertyDescriptor(name = JWT_SIGNING_KEY, label = "The signing key for JWT validation", uiType = ConfigPropertyDescriptor.UI_TYPE.TEXT),
        @ConfigPropertyDescriptor(name = USER_NAME_KEY, label = "The JSON key of the user name", defaultValue = "name", hidden = true),
        @ConfigPropertyDescriptor(name = USER_ROLE_KEY, label = "The JSON key of the user role", defaultValue = "role", hidden = true),
        @ConfigPropertyDescriptor(name = InternalAuthConfig.ALLOW_ANONYMOUS_ACCESS_KEY, label = "The JSON key of the user role", defaultValue = "false", hidden = true)
})
public class InternalAuthConfig extends BundleConfigDefault implements AuthConfig {

    static final String ALLOW_ANONYMOUS_ACCESS_KEY = "allowAnonymousAccess";

    @Override
    public boolean isJwt() {
        return true;
    }

    @Override
    public String getJwtSigningKey() {
        return Strings.nullToEmpty(properties.get(JWT_SIGNING_KEY));
    }

    @Override
    public void setJwtSigningKey(String key) {
        properties.put(JWT_SIGNING_KEY, key);
        try {
            save();
        } catch (IOException e) {
            //ignore
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getUserNameKey() {
        return Strings.nullToEmpty(properties.get(USER_NAME_KEY));
    }

    @Override
    public String getUserRoleKey() {
        return Strings.nullToEmpty(properties.get(USER_ROLE_KEY));
    }

    public boolean isAnonymousAccessAllowed() {
        return Boolean.parseBoolean(properties.get(ALLOW_ANONYMOUS_ACCESS_KEY));
    }
}
