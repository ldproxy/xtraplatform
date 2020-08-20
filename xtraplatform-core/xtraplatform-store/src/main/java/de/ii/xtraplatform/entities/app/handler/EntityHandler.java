/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app.handler;

import com.google.common.base.Strings;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationListener;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by zahnen on 27.11.15.
 * <p>
 * level has to be smaller than the ones of ConfigurationHandler and ProvidedServiceHandler
 */
@Handler(name = "Entity", namespace = EntityHandler.NAMESPACE, level = 0)
public class EntityHandler extends PrimitiveHandler implements ConfigurationListener {

    static final String NAMESPACE = "de.ii.xtraplatform.entities.domain.handler";
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHandler.class);

    private ProvidedServiceHandler providedServiceHandler;

    @Override
    public void initializeComponentFactory(ComponentTypeDescription typeDesc,
                                           Element metadata) throws ConfigurationException {

        // check if class implements PersistentEntity
        Element[] inherited = typeDesc.getDescription()
                                      .getElements("inherited");
        if (inherited == null || inherited.length == 0 || !inherited[0].containsAttribute("interfaces") || !inherited[0].getAttribute("interfaces")
                                                                                                                        .contains(PersistentEntity.class.getName())) {
            throw new IllegalStateException("The class " + getPojoMetadata().getClassName() + " does not extend " + PersistentEntity.class.getName());
        }


        // read type, subType , dataClass and dataSubClass from annotation
        Element[] bundleConfigElements = metadata.getElements(Entity.class.getSimpleName()
                                                                          .toLowerCase(), NAMESPACE);

        Optional<String> type = Optional.ofNullable(Strings.emptyToNull(bundleConfigElements[0].getAttribute(Entity.TYPE_KEY)));
        Optional<String> subType = Optional.ofNullable(Strings.emptyToNull(bundleConfigElements[0].getAttribute(Entity.SUB_TYPE_KEY)));
        Optional<String> dataClass = Optional.ofNullable(Strings.emptyToNull(bundleConfigElements[0].getAttribute(Entity.DATA_CLASS_KEY)));
        Optional<String> dataSubClass = Optional.ofNullable(Strings.emptyToNull(bundleConfigElements[0].getAttribute(Entity.DATA_SUB_CLASS_KEY)));

        if (!type.isPresent()) {
            throw new IllegalStateException("type not set for Entity");
        }

        if (!dataClass.isPresent()) {
            throw new IllegalStateException("dataClass not set for Entity");
        }


        // add @ServiceController for field register in class AbstractPersistentEntity
        Element[] providedServices = metadata.getElements("Provides");
        if (providedServices == null || providedServices.length == 0) {
            Element provides = new Element("Provides", null);
            metadata.addElement(provides);
            providedServices = metadata.getElements("Provides");
        }
        Element controller = new Element("controller", null);
        providedServices[0].addElement(controller);
        controller.addAttribute(new Attribute("field", "register"));
        controller.addAttribute(new Attribute("value", "false"));


        // add @Property(name = Entity.DATA_KEY) for method setData in class AbstractPersistentEntity
        Element properties;
        Element[] confs = metadata.getElements("Properties", "");
        if (confs == null || confs.length == 0) {
            properties = new Element("Properties", null);
            metadata.addElement(properties);
        } else {
            properties = confs[0];
        }
        Element data = new Element("property", null);
        properties.addElement(data);
        data.addAttribute(new Attribute("name", Entity.DATA_KEY));
        data.addAttribute(new Attribute("method", "setData"));
        data.addAttribute(new Attribute("type", EntityData.class.getName()));



        // add type, subType, dataClass and dataSubClass to type description
        typeDesc.addProperty(new PropertyDescription(Entity.TYPE_KEY, String.class.getName(), type.get(), true));
        if (subType.isPresent()) {
            typeDesc.addProperty(new PropertyDescription(Entity.SUB_TYPE_KEY, String.class.getName(), subType.get(), true));
        }
        typeDesc.addProperty(new PropertyDescription(Entity.DATA_CLASS_KEY, String.class.getName(), dataClass.get(), true));
        if (dataSubClass.isPresent() && !Objects.equals(dataSubClass.get(), "java.lang.Object")) {
            typeDesc.addProperty(new PropertyDescription(Entity.DATA_SUB_CLASS_KEY, String.class.getName(), dataSubClass.get(), true));
        }


        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("ENTITY {}", metadata);
            LOGGER.trace("HANDLERS {} {}", typeDesc.getFactory()
                                                   .getRequiredHandlers(), Arrays.stream(metadata.getElements())
                                                                                 .map(Element::getName)
                                                                                 .collect(Collectors.toList()));
        }
    }

    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {

    }

    @Override
    public void stop() {

    }

    @Override
    public void start() {
        ConfigurationHandler configurationHandler = (ConfigurationHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":properties");
        configurationHandler.addListener(this);
        this.providedServiceHandler = (ProvidedServiceHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":provides");
    }

    @Override
    public void configurationChanged(ComponentInstance instance, Map<String, Object> configuration) {
        // TODO: could directly ask shouldRegister()
        try {
            Field field = getInstanceManager().getPojoObject()
                                              .getClass()
                                              .getField("register");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            boolean register = (boolean) field.get(getInstanceManager().getPojoObject());
            //LOGGER.debug("UPDATE {}", register);

            providedServiceHandler.onSet(null, "register", register);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
            //LOGGER.error("ERR", e);
        }

    }
}