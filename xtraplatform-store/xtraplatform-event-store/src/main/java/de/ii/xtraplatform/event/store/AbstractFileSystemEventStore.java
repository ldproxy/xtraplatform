package de.ii.xtraplatform.event.store;

import akka.stream.ActorMaterializer;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.dropwizard.api.StoreConfiguration;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import de.ii.xtraplatform.runtime.FelixRuntime;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

abstract class AbstractFileSystemEventStore extends AbstractEventStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileSystemEventStore.class);
    static final String STORE_DIR_LEGACY = "config-store";

    protected final boolean isEnabled;
    private final Path storeDirectory;
    private final List<Path> additionalDirectories;
    final FileSystemEvents eventReaderWriter;
    private final StoreConfiguration storeConfiguration;


    AbstractFileSystemEventStore(BundleContext bundleContext, XtraPlatform xtraPlatform,
                                 ActorSystemProvider actorSystemProvider, boolean isEnabled) {
        super(isEnabled ? ActorMaterializer.create(actorSystemProvider.getActorSystem(bundleContext)) : null);
        this.storeDirectory = getStoreDirectory(bundleContext.getProperty(FelixRuntime.DATA_DIR_KEY), xtraPlatform.getConfiguration().store);
        this.additionalDirectories = getAdditionalDirectories(bundleContext.getProperty(FelixRuntime.DATA_DIR_KEY), xtraPlatform.getConfiguration().store);
        this.eventReaderWriter = isEnabled ? new FileSystemEvents(storeDirectory, xtraPlatform.getConfiguration().store.instancePathPattern, xtraPlatform.getConfiguration().store.overridesPathPatterns) : null;
        this.isEnabled = isEnabled;
        this.storeConfiguration = xtraPlatform.getConfiguration().store;
    }

    private Path getStoreDirectory(String dataDir, StoreConfiguration storeConfiguration) {
        String storeLocation = storeConfiguration.location;
        if (Paths.get(storeLocation).isAbsolute()) {
            if (storeConfiguration.mode == StoreConfiguration.StoreMode.READ_WRITE && !storeLocation.startsWith(dataDir)) {
                //not allowed?
                throw new IllegalStateException(String.format("Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).", storeLocation, dataDir));
            }
            return Paths.get(storeLocation);
        }

        return Paths.get(dataDir, storeLocation);
    }

    private List<Path> getAdditionalDirectories(String dataDir, StoreConfiguration storeConfiguration) {
        ImmutableList.Builder<Path> additionalDirectories = new ImmutableList.Builder<>();

        for (String storeLocation: storeConfiguration.additionalLocations) {
            if (Paths.get(storeLocation)
                     .isAbsolute()) {
                if (storeConfiguration.mode == StoreConfiguration.StoreMode.READ_WRITE && !storeLocation.startsWith(dataDir)) {
                    //not allowed?
                    throw new IllegalStateException(String.format("Invalid store location (%s). READ_WRITE stores must reside inside the data directory (%s).", storeLocation, dataDir));
                }
                additionalDirectories.add(Paths.get(storeLocation));
            } else {
                additionalDirectories.add(Paths.get(dataDir, storeLocation));
            }
        }

        return additionalDirectories.build();
    }

    //TODO: middleware for path transformations, e.g. multitenancy

    protected final void replay() {
        LOGGER.info("Store location: {}", storeDirectory.toAbsolutePath());

        if (!additionalDirectories.isEmpty()) {
            LOGGER.info("Additional store locations: {}", additionalDirectories.stream().map(Path::toAbsolutePath).collect(Collectors.toList()));
        }

        createOrMigrateStore();

        if (Files.exists(storeDirectory)) {
            try {
                eventReaderWriter.loadEventStream()
                                 .forEach(this::emit);
            } catch (Throwable e) {
                LOGGER.error("Reading events from '{}' failed: {}", storeDirectory, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace:", e);
                }
            }
        }

        for (Path additionalDirectory: additionalDirectories) {
            if (Files.exists(additionalDirectory)) {
                FileSystemEvents fileSystemEvents = new FileSystemEvents(additionalDirectory, storeConfiguration.instancePathPattern, storeConfiguration.overridesPathPatterns);
                try {
                    fileSystemEvents.loadEventStream()
                                     .forEach(this::emit);
                } catch (Throwable e) {
                    LOGGER.error("Reading events from '{}' failed: {}", additionalDirectory, e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Stacktrace:", e);
                    }
                }
            }
        }

        //replay done
        onStart();
    }

    private void createOrMigrateStore() {
        try {
            boolean usingNewStore = Files.isDirectory(storeDirectory) && Files.list(storeDirectory)
                                                                              .findFirst()
                                                                              .isPresent();
            Files.createDirectories(storeDirectory);

            Path legacyStoreDirectory = storeDirectory.getParent()
                                                      .resolve(STORE_DIR_LEGACY);
            boolean usingLegacyStore = Files.isDirectory(legacyStoreDirectory) && Files.list(legacyStoreDirectory)
                                                                                       .findFirst()
                                                                                       .isPresent();

            if (usingLegacyStore) {
                if (usingNewStore) {
                    LOGGER.warn("Found non-empty stores in '{}' and '{}'. Please merge manually and remove '{}'.", legacyStoreDirectory.toAbsolutePath(), storeDirectory.toAbsolutePath(), legacyStoreDirectory.toAbsolutePath());
                } else {
                    migrateStore(legacyStoreDirectory);
                }
            }
        } catch (IOException e) {

        }
    }

    //TODO: settings, ldproxy-services
    private void migrateStore(Path legacyStoreDirectory) {
        try {
            List<Path> directoriesToDelete = new ArrayList<>();

            Files.walk(legacyStoreDirectory)
                 .forEach(fileOrDirectory -> {
                     try {
                         Path newFileOrDirectory = storeDirectory.resolve(legacyStoreDirectory.relativize(fileOrDirectory));
                         if (Files.isDirectory(fileOrDirectory)) {
                             if (Files.list(fileOrDirectory).findFirst().isPresent()) {
                                 LOGGER.debug("Creating directory {}", newFileOrDirectory);
                                 Files.createDirectories(newFileOrDirectory);
                             }
                             directoriesToDelete.add(0, fileOrDirectory);
                         } else {
                             LOGGER.debug("Copying File {}", newFileOrDirectory);
                             Files.copy(fileOrDirectory, newFileOrDirectory);// use flag to override existing
                             Files.delete(fileOrDirectory);
                         }
                     } catch (Exception e) {
                         throw new IllegalStateException(e.getMessage());
                     }
                 });

            for (Path path : directoriesToDelete) {
                Files.delete(path);
            }

            LOGGER.info("Migrated store from '{}' to '{}'", legacyStoreDirectory.toAbsolutePath(), storeDirectory.toAbsolutePath());
        } catch (Throwable e) {
            LOGGER.error("Error migrating store from '{}' to '{}': {}", legacyStoreDirectory.toAbsolutePath(), storeDirectory.toAbsolutePath(), e.getMessage());
        }
    }
}
