package de.ii.xtraplatform.feature.core;


import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.entity.api.PersistentEntity;
import de.ii.xtraplatform.entity.repository.EntityInstantiator;
import de.ii.xtraplatform.entity.repository.ServiceTest;
import de.ii.xtraplatform.service.api.ImmutableFeatureProviderExample;
import de.ii.xtraplatform.service.api.ImmutableServiceData;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiAssert;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;

@RunWith(PaxExam.class)
public class TestEntityRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEntityRepository.class);

    private static final List<String> testBundles = ImmutableList.<String>builder()
            .add("xtraplatform-entity-api")
            .add("xtraplatform-entity-repository")
            .add("xtraplatform-service-api")
            .add("xtraplatform-api")
            .add("xtraplatform-kvstore-api")
            .add("xtraplatform-kvstore-inmemory")
            //.add("xtraplatform-config-store-api")
            .add("xtraplatform-dropwizard")
            .build();

    @Inject
    private BundleContext context;

    //@Inject
    private EntityRepository entityRepository;

    private OSGiHelper osgi;
    private IPOJOHelper ipojo;

    //@Inject @Filter("(sophistication=major)")
    //private CoolService coolService;

    //@Rule
    //public MethodRule rule = new MyMethodRule();

    @Configuration
    public Option[] config1() throws IOException {
        //TODO: generate a file with local bundle urls, load it here
        Properties properties = new Properties();
        File bundles = new File("build/classes/test/META-INF/maven/dependencies.properties");
        System.out.println("BUNDLES: " + bundles.getAbsolutePath());
        properties.load(new FileReader(bundles));

        //bundle(new File('./../out/production/Filter4osgi.jar').toURI().toString())
        DefaultCompositeOption defaultCompositeOption = new DefaultCompositeOption(junitBundles());

        Arrays.stream(properties.getProperty("bundles")
                                .split(","))
              .filter(bundle -> !Strings.isNullOrEmpty(bundle))
              .filter(bundle -> !(bundle.contains("xtraplatform") || bundle.contains("felix.http")) || testBundles.stream().anyMatch(bundle::contains))
              .map(CoreOptions::bundle)
              .forEach(defaultCompositeOption::add)
        //.forEach(b -> System.out.println(b))
        ;

        // for dropwizard
        defaultCompositeOption.add(CoreOptions.systemPackages("sun.reflect", "sun.misc"));
        defaultCompositeOption.add(CoreOptions.frameworkProperty("de.ii.xtraplatform.directories.data").value("src/test/resources"));

        return defaultCompositeOption.getOptions();
    }

    @Before
    public void setUp() {
        System.out.println("BC: " + context.getBundle()
                                           .getSymbolicName());

        osgi = new OSGiHelper(context) {
            @Override
            public <T> T waitForService(Class<T> itf, String filter, long timeout) {
                try {
                    return super.waitForService(itf, filter, timeout);
                } catch (AssertionError e) {
                    throw new AssertionError(itf.getName() + ": no matching service found after timeout of " + timeout + "ms");
                }
            }
        };
        ipojo = new IPOJOHelper(context);
    }

    @After
    public void tearDown() {
        osgi.dispose();
        ipojo.dispose();
    }

    @Test
    public void test1() {
        OSGiAssert osgiAssert = new OSGiAssert(context);

        // did all required bundles start?
        testBundles.forEach(bundle -> osgiAssert.assertBundleState(bundle, Bundle.ACTIVE));

        // did all required services start?
        osgi.waitForService(KeyValueStore.class, null, 1000);
        osgi.waitForService(Jackson.class, null, 1000);
        this.entityRepository = osgi.waitForService(EntityRepository.class, null, 1000);
        osgi.waitForService(EntityInstantiator.class, null, 1000);
LOGGER.debug("REPO {}", entityRepository);
        // create two TestService entities
        createEntity("foo", true);
        createEntity("bar", false);

        //TODO instance is either in registry or in factory, but not both
        osgi.waitForService(PersistentEntity.class.getName(), null, 1000, false);

        Factory factory = ipojo.getFactory(ServiceTest.class.getName());
        LOGGER.debug("INSTANCES {} {} {}", factory.getInstancesNames(), factory.getComponentDescription()
                                                                               .getprovidedServiceSpecification(), osgi.isServiceAvailable(PersistentEntity.class));

        String instanceName = ServiceTest.class.getName() + "/" + "foo";
        //ComponentInstance instance = ipojo.getInstanceByName(instanceName);
       /* Optional<ComponentInstance> instance = factory.getInstances()
                                                      .stream()
                                                      .filter(i -> i.getInstanceName()
                                                                    .equals(instanceName))
                                                      .findFirst();
        assertTrue("instance not found: " + instanceName, instance.isPresent());
*/
        assertTrue("instance not valid: " + instanceName, ipojo.isInstanceValid(instanceName));


        //assertEquals("It's so hot!", entityInstantiator.getResult());
    }

    private void createEntity(String id, boolean shouldStart) {
        try {
            new EntityRepositoryForType(entityRepository, ServiceTest.class.getName()).createEntity(ImmutableServiceData.builder()
                                                                                                                        .id(id)
                                                                                                                        .label(id.toUpperCase())
                                                                                                                        .createdAt(Instant.now()
                                                                                                                                          .toEpochMilli())
                                                                                                                        .lastModified(Instant.now()
                                                                                                                                             .toEpochMilli())
                                                                                                                        .serviceType("WFS3")
                                                                                                                        .featureProviderData(ImmutableFeatureProviderExample.builder()
                                                                                                                                                                            .useBasicAuth(false)
                                                                                                                                                                            .build())
                                                                                                                        // REGISTRATION
                                                                                                                        .shouldStart(shouldStart)
                                                                                                                        .build());
        } catch (IOException e) {
            LOGGER.debug("CREATE ERROR", e);
        }

    }
}