/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public interface Service {

    public enum STATUS {

        STOPPED,
        STOPPING,
        STARTING,
        STARTED
    }
    
    @JsonView(JsonViews.DefaultView.class)
    public Long getDateCreated();

    public void setDateCreated(long dateCreated);

    @JsonView({JsonViews.DefaultView.class,JsonViews.ConfigurationView.class})
    public Long getLastModified();

    public void setLastModified(long lastModified);

    @JsonView(JsonViews.DefaultView.class)
    public String getId();

    @JsonView(JsonViews.DefaultView.class)
    public String getType();
    
    @JsonView({JsonViews.DefaultView.class,JsonViews.ConfigurationView.class})
    public String getName();

    @JsonView(JsonViews.ConfigurationView.class)
    public String getDescription();
    
    @JsonIgnore
    public File getConfigDirectory();

    public void setId(String id);

    public void setType(String type);
    
    public void setName(String name);

    public void setDescription(String description);
    
    public void setConfigDirectory(File directory);

    @JsonView(JsonViews.AdminView.class)
    public String getInterfaceSpecification();

    @JsonView(JsonViews.AdminView.class)
    public String getBrowseUrl();

    @JsonView(JsonViews.AdminView.class)
    public Map<String,List<Notification>> getNotifications();

    @JsonView(JsonViews.ConfigurationView.class)
    public STATUS getTargetStatus();

    public void setTargetStatus(STATUS status);
    
    @JsonView(JsonViews.AdminView.class)
    public STATUS getStatus();

    public void start();

    public void stop();

    @JsonIgnore
    public boolean isStarted();
   
    public void update(String customConfig);
    
    public void load() throws IOException;
    
    public void save();
    
    public void delete();
    
    public void invalidate();
}
