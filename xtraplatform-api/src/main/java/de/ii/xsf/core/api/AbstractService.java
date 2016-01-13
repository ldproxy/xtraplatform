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
package de.ii.xsf.core.api;

import de.ii.xsf.logging.XSFLogger;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public abstract class AbstractService implements Service, Comparable <AbstractService> {

    private static final String SERVICE_CONFIG_FILE_NAME = "-service.json";
    private static final String CUSTOM_CONFIG_FILE_NAME = "-customconfig.json";
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractService.class);
    protected String id;
    protected String type;
    protected String name;
    protected String description;
    protected File configDirectory;
    protected Service.STATUS status;
    protected Service.STATUS targetStatus;
    protected long dateCreated;
    protected long lastModified;

    public AbstractService(String id, String type, File configDirectory) {
        this();
        this.id = id;
        this.type = type;
        this.name = "default";
        this.configDirectory = configDirectory;
    }

    public AbstractService() {
        this.targetStatus = Service.STATUS.STARTED;
        this.status = Service.STATUS.STOPPED;
    }

    @Override
    public long getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified; 
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }
   
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public File getConfigDirectory() {
        return configDirectory;
    }

    @Override
    public STATUS getTargetStatus() {
        return targetStatus;
    }
    
    @Override
    public STATUS getStatus() {
        return status;
    }
    
    @Override
    public void setTargetStatus(STATUS status){
        this.targetStatus = status;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setConfigDirectory(File directory) {
        this.configDirectory = directory;
    }

    @Override
    final public void start() {
        this.status = Service.STATUS.STARTING;
        internalStart();
        this.status = Service.STATUS.STARTED;
    }

    @Override
    final public void stop() {
        this.status = Service.STATUS.STOPPING;
        internalStop();
        this.status = Service.STATUS.STOPPED;
    }

    @Override
    public Map<String,List<Notification>> getNotifications() {
        Map<String,List<Notification>> map = new HashMap<>();
        map.put("en", new ArrayList<Notification>());
        return map;
    }

    @Override
    public boolean isStarted() {
        return status == Service.STATUS.STARTED;
    }

    abstract protected void internalStart();

    abstract protected void internalStop();
    
    @Override
    public int compareTo(AbstractService t) {
        
        return (int) (this.getDateCreated() - t.getDateCreated());
        
    }
    
}
