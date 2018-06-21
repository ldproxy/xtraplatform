package de.ii.xtraplatform.entity.repository;

import de.ii.xtraplatform.entity.api.AbstractEntityData;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryChangeListener;
import de.ii.xtraplatform.service.api.ImmutableServiceData;
import de.ii.xtraplatform.service.api.ModifiableServiceData;
import de.ii.xtraplatform.service.api.ServiceData;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.handlers.event.Subscriber;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */

@Component(publicFactory = false)
@Provides(specifications = {EntityInstantiator.class})
@Instantiate
@Wbp(
        filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.entity.api.PersistentEntity))",
        onArrival = "onFactoryArrival",
        onDeparture = "onFactoryDeparture")
public class EntityInstantiator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInstantiator.class);

    @Context
    private BundleContext context;

    @Requires
    private DeclarationBuilderService declarationBuilderService;

    private final EntityRepository entityRepository;
    private final Map<String, DeclarationHandle> instanceHandles;
    private final Map<String, Factory> componentFactories;
    private final Map<String, String> entityTypes;
    private final Map<String, AbstractEntityData> entityBuffer;

    public EntityInstantiator(@Requires EntityRepository entityRepository) {
        this.instanceHandles = new LinkedHashMap<>();
        this.componentFactories = new LinkedHashMap<>();
        this.entityTypes = new LinkedHashMap<>();
        this.entityBuffer = new LinkedHashMap<>();
        this.entityRepository = entityRepository;
        //entityRepository.addChangeListener(this);
    }

    public String getResult() {
        return "It's so hot!";
    }

    private synchronized void onFactoryArrival(ServiceReference<Factory> ref) {
        Optional<String> entityType = Optional.ofNullable((String)ref.getProperty("component.class"));
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                            .filter(pd -> pd.getName().equals("data"))
                                            .map(PropertyDescription::getType)
                                            .findFirst();

        if (entityType.isPresent() && entityDataType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY FACTORY {} {}", entityDataType.get(), entityType.get());
            }
            this.componentFactories.put(entityDataType.get(), context.getService(ref));
            this.entityTypes.put(entityDataType.get(), entityType.get());

            initInstances(entityType.get());
        }


    }

    private synchronized void onFactoryDeparture(ServiceReference<Factory> ref) {
        Optional<String> entityType = Optional.ofNullable((String)ref.getProperty("component.class"));
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName().equals("data"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();

        if (entityType.isPresent() && entityDataType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REMOVE ENTITY FACTORY {} {}", entityDataType.get(), entityType.get());
            }
            this.componentFactories.remove(entityDataType.get());
            this.entityTypes.remove(entityDataType.get());

            clearInstances(entityType.get());
        }
    }

    private Optional<String> getEntityType(AbstractEntityData data) {
        return Optional.ofNullable(entityTypes.get(data.getClass().getName().replace(".Immutable", ".")));
    }

    @Subscriber(name = "create", topics = "create", dataKey = "data", dataType = "de.ii.xtraplatform.entity.api.AbstractEntityData")
    public void onEntityCreate(AbstractEntityData data) {
        LOGGER.debug("TYPES {} {}", entityTypes, data.getClass().getName().replace(".Immutable", "."));
        Optional<String> entityType = getEntityType(data);
        if (entityType.isPresent()) {
            createInstance(entityType.get(), data.getId(), data);
        } else {
            entityBuffer.put(data.getClass().getName(), data);
        }
    }

    @Subscriber(name = "update", topics = "update", dataKey = "data", dataType = "de.ii.xtraplatform.entity.api.AbstractEntityData")
    public void onEntityUpdate(AbstractEntityData data) {
        updateInstance(getEntityType(data).get(), data.getId(), data);
    }

    @Subscriber(name = "delete", topics = "delete", dataKey = "data", dataType = "java.lang.String")
    public void onEntityDelete(String id) {
        deleteInstance(id);
    }

    private void initInstances(String type) {
        LOGGER.debug("INIT ENTITIES {} {}", type, entityRepository.getEntityIds());

        for (String id : entityRepository.getEntityIds()) {
            createInstance(type, id, entityRepository.getEntityData(id));
        }
    }

    private void clearInstances(String type) {
        LOGGER.debug("CLEAR ENTITIES {}", type);

        for (String id : instanceHandles.keySet()) {
            if (id.startsWith(type + "/")) {
                deleteInstance(id);
            }
        }
    }

    private void createInstance(String type, String id, AbstractEntityData data) {
        LOGGER.debug("CREATE ENTITY {} {} {}", type, id, data);

        InstanceBuilder instanceBuilder = declarationBuilderService.newInstance(type);

        // TODO: type/id
        DeclarationHandle handle = instanceBuilder.name(type+ "/" + id)
                                                  .configure()
                                                  // simulate deserialization to Modifiable
                                                  .property("data", ModifiableServiceData.create().from((ServiceData) data))
                                                  .property("organization", "ORG")
                                                  .build();

        handle.publish();

        this.instanceHandles.put(id, handle);
    }

    private void updateInstance(String type, String id, AbstractEntityData data) {
        LOGGER.debug("UPDATE ENTITY {} {} {}", type, id, data);

        if (componentFactories.containsKey(type) && instanceHandles.containsKey(id)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", id);
            configuration.put("data", data);

            try {
                componentFactories.get(type)
                                  .reconfigure(configuration);
            } catch (UnacceptableConfiguration | MissingHandlerException unacceptableConfiguration) {
                //ignore
            }
        }
    }

    private void deleteInstance(String id) {
        LOGGER.debug("DELETE ENTITY {}", id);

        if (instanceHandles.containsKey(id)) {
            instanceHandles.get(id)
                           .retract();
            instanceHandles.remove(id);
        }
    }
}
