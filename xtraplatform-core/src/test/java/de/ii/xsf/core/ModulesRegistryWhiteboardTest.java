/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xsf.core;

import de.ii.xsf.core.api.ServiceModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author fischer
 */
public class ModulesRegistryWhiteboardTest {
    
    @InjectMocks
    private ModulesRegistryWhiteboard mrw;
    
    @Mock
    private BundleContext context;
    
    @Mock
    private ServiceModule module;
    
    public ModulesRegistryWhiteboardTest() {
    }
    
    @BeforeClass(groups = {"default"})
    public void init() throws IOException{
        MockitoAnnotations.initMocks(this);
        
        Mockito.when(context.getService(null)).thenReturn(module);
         Mockito.when(module.getName())
                .thenReturn("TESTMODULE_1")
                .thenReturn("TESTMODULE_2")
                .thenReturn("TESTMODULE_3")
                .thenReturn("TESTMODULE_4")
                .thenReturn("TESTMODULE_5")
                .thenReturn("TESTMODULE_6")
                .thenReturn("TESTMODULE_7")
                .thenReturn("TESTMODULE_8")
                .thenReturn("TESTMODULE_9")
                .thenReturn("TESTMODULE_0");
    }

    @Test(groups = {"default"})
    public void testSomeMethod() {
        
        
        try {
            
            this.test(10);
            
        } catch (InterruptedException ex) {
            Logger.getLogger(ModulesRegistryWhiteboardTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ModulesRegistryWhiteboardTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private void test(final int threadCount) throws InterruptedException, ExecutionException {

        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() {
                mrw.onArrival(null);
                return null;
            }
        };

        List<Callable<Void>> tasks = Collections.nCopies(threadCount, task);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        List<Future<Void>> futures = executorService.invokeAll(tasks);
        
        List<Void> resultList = new ArrayList<Void>(futures.size());
        // Check for exceptions
        List<Exception> exceptions = new ArrayList<Exception>();
        for (Future<Void> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            try {
                resultList.add(future.get());
            } catch (Exception e) {
                exceptions.add(e);
                e.printStackTrace();
                System.out.println("EXCEPTION");
            }
        }

        Assert.assertEquals(new ArrayList<Exception>(), exceptions);
        Assert.assertEquals(threadCount, futures.size());

    }
}
