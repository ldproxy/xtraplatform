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
package de.ii.xsf.core.api.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xsf.core.api.Notification;
import de.ii.xsf.core.api.Service;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class AdminServiceWrapper implements Service{

    private Service service;
    
    public AdminServiceWrapper(Service service) {
        this.service = service;
    }

    @Override
    public String getId() {
        return service.getId();
    }

    @Override
    public String getType() {
        return service.getType();
    }

    @Override
    @JsonIgnore
    public String getInterfaceSpecification() {
        return service.getInterfaceSpecification();
    }

    @Override
    @JsonIgnore
    public File getConfigDirectory() {
        return service.getConfigDirectory();
    }

    @Override
    public void setId(String id) {
        
    }

    @Override
    public void setType(String type) {
        
    }

    @Override
    public void setConfigDirectory(File directory) {
        
    }

    @Override
    public STATUS getStatus() {
        return service.getStatus();
    }
    
    @Override
    public void start() {
        
    }

    @Override
    public void stop() {
        
    }

    @Override
    public String getBrowseUrl() {
        return "/rest/services/" + service.getBrowseUrl();
    }
    
    public String getMapAppsUrl() {
        return "/map.apps/?app=" + service.getId();
    }

    @Override
    public Map<String,List<Notification>> getNotifications() {
        return service.getNotifications();
    }

    @JsonIgnore
    @Override
    public boolean isStarted() {
       return service.isStarted();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDescription(String type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(String customConfig) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void load() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        return service.getName();
    }

    @Override
    public void setName(String name) {
        service.setName(name);
    }

    @Override
    public Long getDateCreated() {
        return service.getDateCreated();
    }

    @Override
    public void setDateCreated(long dateCreated) {
        
    }

    @Override
    public Long getLastModified() {
        return service.getLastModified();
    }

    @Override
    public void setLastModified(long lastModified) {
        
    }

    @Override
    public void invalidate() {
        
    }

    @Override
    public STATUS getTargetStatus() {
        return service.getTargetStatus();
    }

    @Override
    public void setTargetStatus(STATUS status) {
        
    }
}
