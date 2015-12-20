package de.ii.xsf.core;

import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.ServiceModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author fischer
 */
public class ServiceRegistryDefaultTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryDefaultTest.class);

    @InjectMocks
    private ServiceRegistryDefault srd;

    @Mock
    private BundleContext context;

    @Mock
    private ServiceModule module;
    
    @Mock
    private Service srv;
    

    @org.testng.annotations.BeforeClass(groups = {"default"})
    public void init() throws IOException{
        MockitoAnnotations.initMocks(this);

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
        
        Mockito.when(srv.getId())
                .thenReturn("TESTSERVICE_1")
                .thenReturn("TESTSERVICE_2")
                .thenReturn("TESTSERVICE_3")
                .thenReturn("TESTSERVICE_4")
                .thenReturn("TESTSERVICE_5")
                .thenReturn("TESTSERVICE_6")
                .thenReturn("TESTSERVICE_7")
                .thenReturn("TESTSERVICE_8")
                .thenReturn("TESTSERVICE_9")
                .thenReturn("TESTSERVICE_0");

        Mockito.when(context.getService(null)).thenReturn(module);
                       
        List<Service> srvs = new ArrayList<>();
        for (int s = 0; s < 10; s++) {
            srv.setId("sid_"+s);
            srvs.add(srv);
        }
        
        Map<String, List<Service>> modServices = new HashMap<String, List<Service>>();
        for (int i = 0; i < 10; i++) {
            modServices.put("srv_"+i, srvs);
        }
        
        
        Mockito.when(module.getServices()).thenReturn(modServices);
    }

    @Test(groups = {"default"})
    public void testServiceRegistryDefault() throws IOException {

        
        try {
            
            this.test(1000);
            
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(ServiceRegistryDefaultTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(ServiceRegistryDefaultTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        verify(context, atLeastOnce()).getService(null);
    }

    private void test(final int threadCount) throws InterruptedException, ExecutionException {

        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() {
                srd.onModuleArrival(null);
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
                System.out.println("EXCEPTION");
            }
        }

        Assert.assertEquals(new ArrayList<Exception>(), exceptions);
        Assert.assertEquals(threadCount, futures.size());

    }
}
