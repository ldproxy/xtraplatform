package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.dropwizard.domain.AdminSubEndpoint;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityStoreDecorator;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class AdminEndpointEntities implements AdminSubEndpoint {

    enum Status {HEALTHY, ACTIVE, FAILED, DISABLED}

    private final HttpServlet servlet;
    private final EntityDataStore<?> entityDataStore;
    private final EntityRegistry entityRegistry;
    private final ObjectMapper objectMapper;

    public AdminEndpointEntities(@Requires EntityDataStore<?> entityDataStore, @Requires EntityRegistry entityRegistry, @Requires Jackson jackson) {
        this.entityDataStore = entityDataStore;
        this.entityRegistry = entityRegistry;
        this.objectMapper = jackson.getDefaultObjectMapper();
        this.servlet = new EntitiesServlet();
    }

    @Override
    public String getPath() {
        return "/entities";
    }

    @Override
    public HttpServlet getServlet() {
        return servlet;
    }

    class EntitiesServlet extends HttpServlet {
        private static final long serialVersionUID = 3772654177231086757L;
        private static final String CONTENT_TYPE = "application/json";
        private static final String CONTENT = "pong";
        private static final String CACHE_CONTROL = "Cache-Control";
        private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

        @Override
        protected void doGet(HttpServletRequest req,
                             HttpServletResponse resp) throws ServletException, IOException {

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader(CACHE_CONTROL, NO_CACHE);
            resp.setContentType(CONTENT_TYPE);

            LinkedHashMap<String, List<Map<String, String>>> entities = entityDataStore.identifiers()
                                                                                       .stream()
                                                                                       .collect(Collectors.groupingBy(identifier -> identifier.path()
                                                                                                                                              .get(0), LinkedHashMap::new, Collectors.mapping(this::getEntityInfo, Collectors.toList())));

            try (PrintWriter writer = resp.getWriter()) {
                objectMapper.writeValue(writer, entities);
            }
        }

        private ImmutableMap<String, String> getEntityInfo(Identifier identifier) {
            //TODO: same id different type
            Optional<PersistentEntity> entity = entityRegistry.getEntity(PersistentEntity.class, identifier.id()).filter(persistentEntity -> Objects.equals(persistentEntity.getType(), identifier.path()
                                                                                                                                                                                                  .get(0)));
            return ImmutableMap.of("id", identifier.id(), "status", entity.isPresent() ? Status.ACTIVE.name() : Status.DISABLED.name());
        }
    }
}
