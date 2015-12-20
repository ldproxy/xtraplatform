package de.ii.xsf.core;

import de.ii.xsf.logging.XSFLogger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.ii.xsf.core.api.AbstractServiceCatalog;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.ServiceCatalog;
import de.ii.xsf.core.api.ServiceModule;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class Services extends AbstractServiceCatalog implements ServiceCatalog {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Services.class);
    
    private static final String SERVICE_CONFIGURATION_SUFFIX = "-service.json";
    //private ModulesBundle modules;
    private Map<String, Service> services;
    private File servicesDir;
    private ObjectMapper jsonMapper;
    private boolean allowServiceReAdding;
    //private List<ServiceCreationListener> serviceCreationListeners;

    public Services() {
        super();
        //this.modules = modules;
        this.services = new HashMap<String, Service>();
        this.servicesDir = null;
        //this.serviceCreationListeners = new ArrayList();

        jsonMapper = new ObjectMapper();
        //jsonMapper.disable(MapperFeature.USE_ANNOTATIONS);
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void initialize(boolean allowServiceReAdding) throws Exception {
        //LOGGER.info(FrameworkMessages.LOADING_SERVICES);
        //servicesDir = new File(modules.getConfigurationDirectory(), "services");
/*
        if (!servicesDir.exists()) {
            servicesDir.mkdir();
        } else {
            loadServices();
        }*/

        if (allowServiceReAdding) {
            this.allowServiceReAdding = true;
            //LOGGER.warn(FrameworkMessages.ENABLED_RE_ADDING_OF_SERVICES_WITH_THE_SAME_ID);
        } else {
            this.allowServiceReAdding = false;
        }
    }

    public void shutdown() {
        //LOGGER.info(FrameworkMessages.SHUTTING_DOWN_SERVICES);
        for (Service s : services.values()) {
            s.stop();
        }
    }

    public void addService(AuthenticatedUser authUser, String type, String id, Map<String, String> queryParams) {
        if (services.containsKey(id)) {
            if (!allowServiceReAdding) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Service already exists").build());
            }
            services.get(id).stop();
        }

        ServiceModule module = null;//(ServiceModule) modules.getModule(type, ServiceModule.class);
        File srvcDir = new File(servicesDir, id);

        if (!srvcDir.exists()) {
            srvcDir.mkdir();
        }
        Service srvc = null;


        try {
            srvc = module.addService(authUser, id, queryParams, srvcDir);
        } catch (IOException ex) {
            //LOGGER.error(FrameworkMessages.IO_ERROR_WHILE_ADDING_SERVICE, ex);
            throw new WebApplicationException();
        }

        services.put(id, srvc);
        srvc.start();
        
        //for (ServiceCreationListener scl : serviceCreationListeners) {
            //scl.serviceCreated(srvc);
        //}
    }
    
    public void deleteService(String id) {
        services.get(id).stop();
        services.get(id).delete();
        services.remove(id);
    }

    @Override
    public List<Map<String, String>> getServices() {
        List<Map<String, String>> srvcs = new ArrayList();
        for (Service s : services.values()) {
            if (s.isStarted()) {
                Map<String, String> srvc = new HashMap();
                srvc.put("name", s.getId());
                srvc.put("type", s.getInterfaceSpecification());
                srvcs.add(srvc);
            }
        }
        return srvcs;
    }

    @JsonIgnore
    public Collection<Service> getServiceCollection() {
        return services.values();
    }

    @JsonIgnore
    public Service getService(String id) {
        if (services.containsKey(id)) {
            return services.get(id);
        } /*else {
            throw new RessourceNotFound("A service with id '" + id + "' is not available.");
        }*/
        return null;
    }

    @JsonIgnore
    public Class getServiceResourceClass(Service service) throws Exception {
        return null;//((ServiceModule) modules.getModule(service.getType(), ServiceModule.class)).getServiceResourceClass();
    }

    @JsonIgnore
    public Class getServiceAdminResourceClass(Service service) throws Exception {
        return null;//((ServiceModule) modules.getModule(service.getType(), ServiceModule.class)).getServiceAdminResourceClass();
    }

    /*
    private void loadServices() throws Exception {


        File[] dirs = servicesDir.listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (d.isDirectory()) {
                    File[] files = d.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (isServiceConfiguration(f)) {
                                ServiceModule module = getServiceConfigurationModule(f);
                                //if (module.isEnabled() && module.isStarted()) {
                                    //LOGGER.debug(FrameworkMessages.MODULE_STARTED, d.getName(), f.getName());

                                    Service service = module.loadService(d);
                                    services.put(service.getId(), service);
                                    service.start();
                                //} else {
                                //    LOGGER.warn(FrameworkMessages.MODULE_SKIPPED, d.getName(), f.getName());
                                //}
                            }
                        }
                    }
                }
            }
        }*/

        /*for (Map.Entry<String, Module> entry : modules.getModules().entrySet()) {
            Module module = entry.getValue();
            if (module instanceof ServiceModule) {
                List<Service> srvs = ((ServiceModule) module).getServiceList();
                for (Service service : srvs) {
                    services.put(service.getId(), service);
                    service.start();
                }
            }
        }*/
   // }

    private boolean isServiceConfiguration(File f) {
        return f.isFile() && f.getName().endsWith(SERVICE_CONFIGURATION_SUFFIX);
    }

    private ServiceModule getServiceConfigurationModule(File f) {
        String type = f.getName().replace(SERVICE_CONFIGURATION_SUFFIX, "");
        ServiceModule module = null;//(ServiceModule) modules.getModule(type, ServiceModule.class);
        
        return module;
    }

    /*public void addServiceCreationListener(ServiceCreationListener serviceCreationListener) {
        serviceCreationListeners.add(serviceCreationListener);
    }*/
}
