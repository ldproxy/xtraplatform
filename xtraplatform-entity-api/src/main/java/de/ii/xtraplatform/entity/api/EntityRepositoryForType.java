package de.ii.xtraplatform.entity.api;

import com.google.common.collect.ObjectArrays;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class EntityRepositoryForType extends EntityRepositoryWrapper {

    private final String entityType;

    public EntityRepositoryForType(EntityRepository entityRepository, String entityType) {
        super(entityRepository);
        this.entityType = entityType;
    }

    @Override
    protected String transformId(String id) {
        return String.format("%s%s%s", entityType, ID_SEPARATOR, id);
    }

    @Override
    protected List<String> transformIds(List<String> ids) {
        return ids.stream()
                  .filter(isTransformed())
                  .map(this::reverseTransformId)
                  .collect(Collectors.toList());
    }

    @Override
    protected String[] transformPath(String id, String... path) {
        return ObjectArrays.concat(path, entityType);
    }

    private Predicate<String> isTransformed() {
        return id -> id.startsWith(entityType + ID_SEPARATOR);
    }

    private String reverseTransformId(String id) {
        return id.substring(entityType.length()+1);
    }
}
