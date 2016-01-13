/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.api.permission;

import com.fasterxml.jackson.annotation.JsonView;
import de.ii.xsf.core.api.JsonViews.AdminView;
import de.ii.xsf.core.api.JsonViews.ConfigurationView;
import de.ii.xsf.core.api.JsonViews.DefaultView;
import de.ii.xsf.core.api.Resource;
import org.joda.time.DateTime;

/**
 *
 * @author fischer
 */
public class Organization implements Resource {

    private String name;
    private String description;
    private boolean active;
    private long expiration;
    private int serviceLimit;
    private String owner;

    public Organization() {
        this.active = true;
        this.expiration = 0;
        this.serviceLimit = 0;
        this.owner = "";
    }

    /**
     *
     * @return
     */
    @JsonView(DefaultView.class)
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     */
    @Override
    @JsonView(DefaultView.class)
    public String getResourceId() {
        return name;
    }

    /**
     *
     * @param id
     */
    @Override
    public void setResourceId(String id) {
        this.name = id;
    }

    /**
     *
     * @return
     */
    @JsonView(DefaultView.class)
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     *
     * @return
     */
    @JsonView(DefaultView.class)
    public boolean isActive() {
        return active;
    }

    /**
     *
     * @param active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     *
     * @return
     */
    @JsonView(ConfigurationView.class)
    public long getExpiration() {
        return expiration;
    }

    /**
     *
     * @param expiration
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    /**
     *
     * @return
     */
    @JsonView(AdminView.class)
    public int getExpiresIn() {
        // TODO: works only for values <= 365
        return expiration == 0 ? 0 : (new DateTime(expiration).minus(new DateTime().getMillis()).dayOfYear().get() - 1);
    }

    /**
     *
     * @param expiresIn
     */
    public void setExpiresIn(int expiresIn) {
        if (expiresIn > 0) {
            this.expiration = new DateTime().plusDays(expiresIn).getMillis();
        }
    }

    /**
     *
     * @return
     */
    @JsonView(DefaultView.class)
    public int getServiceLimit() {
        return serviceLimit;
    }

    /**
     *
     * @param serviceLimit
     */
    public void setServiceLimit(int serviceLimit) {
        this.serviceLimit = serviceLimit;
    }

    /**
     *
     * @return
     */
    @JsonView(ConfigurationView.class)
    public String getOwner() {
        return owner;
    }

    /**
     *
     * @param owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    
}
