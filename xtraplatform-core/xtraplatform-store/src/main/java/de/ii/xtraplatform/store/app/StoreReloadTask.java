package de.ii.xtraplatform.store.app;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableMultimap;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import io.dropwizard.servlets.tasks.Task;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Instantiate
public class StoreReloadTask extends Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreReloadTask.class);



    protected StoreReloadTask(@Requires Dropwizard dropwizard) {
        super("reload-entity");

        dropwizard.getEnvironment().admin().addTask(this);
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        LOGGER.debug("RELOAD {}", parameters);

        Optional<String> entityType = getEntityType(parameters);

        if (!entityType.isPresent()) {
            output.println("No entity type given");
            output.flush();
            return;
        }
        Optional<String> id = getId(parameters);
        boolean reloadAll = getReloadAll(parameters);

        if (!id.isPresent() && !reloadAll) {
            output.println("Neither 'id' nor 'all' given");
            output.flush();
            return;
        }

        //TODO: create EventFilter + trigger reload
    }

    private Optional<String> getEntityType(ImmutableMultimap<String, String> parameters) {
        final List<String> entityTypes = parameters.get("type").asList();
        return entityTypes.isEmpty() ? Optional.empty() : Optional.ofNullable(entityTypes.get(0));
    }

    private Optional<String> getId(ImmutableMultimap<String, String> parameters) {
        final List<String> ids = parameters.get("id").asList();
        return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.get(0));
    }

    private boolean getReloadAll(ImmutableMultimap<String, String> parameters) {
        final List<String> all = parameters.get("all").asList();
        return !all.isEmpty() && Objects.equals(all.get(0), "true");
    }
}
