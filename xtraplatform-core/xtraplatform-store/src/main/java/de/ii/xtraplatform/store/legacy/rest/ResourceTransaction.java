/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.store.legacy.MultiTransaction;
import de.ii.xtraplatform.store.legacy.Transaction;
import de.ii.xtraplatform.store.legacy.WriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author zahnen
 */
public class ResourceTransaction<T extends Resource> extends MultiTransaction implements WriteTransaction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTransaction.class);

    public enum OPERATION {

        ADD,
        UPDATE,
        UPDATE_OVERRIDE,
        DELETE,
        DELETE_ALL
    }

    private  String[] path;
    private  String resourceId;
    protected  OPERATION operation;
    
    private ResourceSerializer<T> serializer;
    private T resource;
    private T emptyResource;
    private String serializedResource;
    private Lock resourceLock;
    private boolean closed;
    protected ResourceCache<T> cache;
    
    public ResourceTransaction() {
        this.closed = true;
    }
    
    public ResourceTransaction(String[] path, String resourceId, OPERATION operation) {
        this();
        this.path = path;
        this.resourceId = resourceId;
        this.operation = operation;
    }

    private void open() throws IOException {
        if (resourceLock == null) {
            LOGGER.error("resourceLock is not set {}", this);
            throw new IOException("resourceLock is not set");
        }
        LOGGER.debug("LOCK RT");

        if (closed) {
            resourceLock.lock();
            this.closed = false;
        }
    }

    // TODO: do we need cache for this?
    protected void validate() {
        LOGGER.debug("VALIDATE RT {} {} {}", operation, path, resourceId);

        switch (operation) {
            case ADD:
                if (cache.hasResource(resourceId)) {
                    throw new IllegalStateException("A resource with id '" + resourceId + "' already exists");
                }
                break;
            case UPDATE:
            case UPDATE_OVERRIDE:
            case DELETE:
                // TODO: necessary?
                if (resourceId == null) {
                    throw new NotFoundException("A resource with id 'NULL' does not exist");
                }
                if (!cache.hasResource(resourceId)) {
                    throw new NotFoundException("A resource with id '" + resourceId + "' does not exist");
                }
                break;
            case DELETE_ALL:
            default:
        }
    }

    @Override
    public void execute() throws IOException {
        // TODO
        if (operation == OPERATION.DELETE_ALL) {
            LOGGER.debug("IGNORE RT {}", operation);
            return;
        }

        open();

        validate();
        
        LOGGER.debug("EXEC RT {} {} {} {} {} {}", operation, path, resourceId, serializedResource, resource, serializer);

        if (operation != OPERATION.DELETE) {
        checkDeSerialization();
        }

        for (Transaction t : transactions) {
            if (operation != OPERATION.DELETE) {

            try {
                    ((WriteTransaction<String>) t).write(serializedResource);
                    LOGGER.debug("WRITE T<String> {}", t);
            } catch (ClassCastException e) {
                try {
                        ((WriteTransaction<T>) t).write(resource);
                        LOGGER.debug("WRITE T<T> {}", t);
                } catch (ClassCastException e2) {
                    // ignore
                        LOGGER.debug("NO WRITE FOR T {}", t);
                    }
                }
            }

            LOGGER.debug("EXECUTE T {}", t);

            t.execute();
        }
    }

    private void checkDeSerialization() throws IOException {
        if (serializedResource == null) {
            if (operation == OPERATION.ADD || operation == OPERATION.UPDATE) {
                this.serializedResource = serializer.serializeAdd(resource);
            } else {
                this.serializedResource = serializer.serializeUpdate(resource);
            }
            LOGGER.debug("SERIALIZE R {}", serializedResource);
        } else if (resource == null) {
            LOGGER.debug("DESERIALIZE R {}", serializedResource);
            this.resource = serializer.deserialize(emptyResource, new StringReader(serializedResource));
        }
    }

    @Override
    public void write(T resource) throws IOException {
        this.resource = resource;

        checkDeSerialization();
    }

    @Override
    public void rollback() {
        LOGGER.debug("ROLLBACK RT {} {} {}", operation, path, resourceId);
        //super.rollback();
        for (Transaction t : transactions) {
            LOGGER.debug("ROLLBACK T {}", t);
            t.rollback();
        }
        close();
    }

    @Override
    public void commit() {
        LOGGER.debug("COMMIT RT {} {} {}", operation, path, resourceId);
        super.commit(); 
        close();
    }

    @Override
    public void close() {
        // TODO: rollback if no commit/rollback happened yet ???
        if (!closed) {
            resourceLock.unlock();
            this.closed = true;
        }
    }

    public OPERATION getOperation() {
        return operation;
    }

    public String[] getPath() {
        return path;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResource() {
        return serializedResource;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setOperation(OPERATION operation) {
        this.operation = operation;
    }

    public void setResource(String resource) {
        this.serializedResource = resource;
    }

    public void setEmptyResource(T emptyResource) {
        this.emptyResource = emptyResource;
    }

    public void setSerializer(ResourceSerializer<T> serializer) {
        this.serializer = serializer;
    }

    public void setResourceLock(Lock resourceLock) {
        this.resourceLock = resourceLock;
    }

    public void setCache(ResourceCache<T> cache) {
        this.cache = cache;
    }

    @Override
    @JsonIgnore
    public List<Transaction> getTransactions() {
        return super.getTransactions(); 
        
    }

    @Override
    public String toString() {
        return "ResourceTransaction {" +
                "\npath=" + Arrays.toString(path) +
                ", \nresourceId=" + resourceId +
                ", \noperation=" + operation +
                ", \nserializedResource=" + serializedResource +
                "\n}";
    }
}
