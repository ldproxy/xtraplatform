/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.kvstore.api.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;

import de.ii.xtraplatform.kvstore.api.WriteTransaction;
import de.ii.xtraplatform.api.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 *
 * @author zahnen
 */
public class AbstractGenericResourceStoreTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenericResourceStoreTest.class);
    private AbstractGenericResourceStore store;

    @Mock
    private KeyValueStore rootConfigStore;

    @Mock
    private KeyValueStore configStore;

    private ObjectMapper objectMapper;

    @org.testng.annotations.BeforeClass(groups = {"default"})
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);

        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        List<String> resourceIds = new ArrayList<>();
        resourceIds.add("1");
        resourceIds.add("2");
        resourceIds.add("3");
        resourceIds.add("4");
        resourceIds.add("5");

        Mockito.when(rootConfigStore.getChildStore("testres")).thenReturn(configStore);
        Mockito.when(configStore.getChildStore(AbstractGenericResourceStore.OVERRIDES_STORE_NAME)).thenReturn(configStore);
        Mockito.when(configStore.getKeys()).thenReturn(resourceIds);

        Mockito.when(configStore.openDeleteTransaction(Matchers.anyString())).thenReturn(new TestTransaction());
        Mockito.when(configStore.openWriteTransaction(Matchers.anyString())).thenReturn(new TestTransaction());

        this.store = new TestStore(rootConfigStore, "testres", objectMapper, true);
        for (String id : resourceIds) {
            store.addResource(new TestResource(id, "test"));
        }

    }

    @Test(groups = {"default"})
    public void testResourceIdCacheThreadSafety() throws IOException {
        List<String> resourceIds = store.getResourceIds();
        LOGGER.info("resourceIdCache: {}", resourceIds);
        for (String id : resourceIds) {
            store.deleteResource(id);
        }
        LOGGER.info("resourceIdCache: {}", store.getResourceIds());

        Assert.assertTrue(store.getResourceIds().isEmpty());
    }

    @Test(groups = {"default"})
    public void testResourceCache() throws IOException {

        LOGGER.info("testResourceCache");

        TestResource res = new TestResource("10", "eins");

        // add 
        this.store.addResource(res);
        TestResource result = (TestResource) this.store.getResource("10");
        LOGGER.info("result: {} {}", result.getResourceId(), result.getContent());
        Assert.assertEquals(res, result);

        // modify
        TestResource res0 = new TestResource("10", "zwei");
        this.store.updateResource(res0);
        TestResource result0 = (TestResource) this.store.getResource("10");
        LOGGER.info("result: {} {}", result0.getResourceId(), result0.getContent());
        Assert.assertEquals("zwei", result0.getContent());

        // delete
        this.store.deleteResource("10");
        TestResource result1 = (TestResource) this.store.getResource("10");
        LOGGER.info("result: {} {}", result1.getResourceId(), result1.getContent());
        Assert.assertNotSame(result0, result1);
    }

    private class TestResource implements Resource {

        private String id;
        private String content;

        public TestResource(String id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String getResourceId() {
            return id;
        }

        public void setResourceId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public class TestTransaction implements WriteTransaction<String> {

        @Override
        public void commit() {
            LOGGER.info("commit");
        }

        @Override
        public void rollback() {
            LOGGER.info("rollback");
        }

        @Override
        public void write(String value) throws IOException {
            
        }

        @Override
        public void close() {

        }

        @Override
        public void execute() throws IOException {
            
        }

    }

    public class TestStore extends AbstractGenericResourceStore<Resource, ResourceStore> implements ResourceStore<Resource> {

        public TestStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper, boolean fullCache) {
            super(rootConfigStore, resourceType, jsonMapper, fullCache);
        }

        @Override
        protected Resource createEmptyResource(String id, String... path) {
            return new TestResource("x", "y");
        }

        @Override
        protected Class<?> getResourceClass(String id, String... path) {
            return TestResource.class;
        }
    }
}
