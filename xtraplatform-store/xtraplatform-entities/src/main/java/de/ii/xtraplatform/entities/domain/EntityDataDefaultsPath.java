package de.ii.xtraplatform.entities.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.store.domain.Identifier;
import org.immutables.value.Value;

import java.util.List;

//TODO: unit tests for all cases
@Value.Modifiable
public interface EntityDataDefaultsPath {

    Splitter DOT_SPLITTER = Splitter.on('.');

    static EntityDataDefaultsPath from(Identifier identifier) {
        ModifiableEntityDataDefaultsPath defaultsPath = ModifiableEntityDataDefaultsPath.create();
        List<String> pathSegments;

        if (identifier.path()
                      .isEmpty()) {
            if (identifier.id()
                          .contains(".")) {
                int firstDot = identifier.id()
                                         .indexOf(".");
                defaultsPath.setEntityType(identifier.id()
                                                     .substring(0, firstDot));
                pathSegments = DOT_SPLITTER.splitToList(identifier.id()
                                                              .substring(firstDot + 1));
            } else {
                defaultsPath.setEntityType(identifier.id());
                pathSegments = ImmutableList.of();
            }
        } else {
            defaultsPath.setEntityType(identifier.path()
                                                 .get(0));
            pathSegments = identifier.path()
                                     .subList(1, identifier.path()
                                                           .size());
        }

        if (!pathSegments.isEmpty()) {
            for (int i = pathSegments.size(); i > 0; i--) {
                try {
                    List<String> subtype = pathSegments.subList(0, i);
                    defaultsPath.setEntitySubtype(subtype);

                    if (!identifier.path()
                                   .isEmpty()) {
                        defaultsPath.setKeyPath(ImmutableList.of(identifier.id()));
                    }
                } catch (Throwable e) {
                    List<String> keyPath = pathSegments.subList(i - 1, pathSegments.size());

                    defaultsPath.setKeyPath(keyPath);
                    defaultsPath.addKeyPath(identifier.id());
                }
            }
        }

        return defaultsPath;
    }

    String getEntityType();

    List<String> getEntitySubtype();

    List<String> getKeyPath();
}
