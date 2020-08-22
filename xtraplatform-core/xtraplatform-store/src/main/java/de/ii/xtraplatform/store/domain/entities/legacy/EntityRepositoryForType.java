/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities.legacy;

import com.google.common.collect.ObjectArrays;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Deprecated // needed for 1.3.x service migration
public class EntityRepositoryForType extends EntityRepositoryWrapper {

    private final String entityType;

    public EntityRepositoryForType(EntityRepository entityRepository, String entityType) {
        super(entityRepository);
        this.entityType = entityType;
    }

    @Override
    protected String transformId(String id) {
        return String.format("%s%s%s", entityType, EntityRepository.ID_SEPARATOR, id);
    }

    @Override
    protected List<String> transformIds(List<String> ids) {
        return ids.stream()
                  //.filter(isTransformed())
                  //.map(this::reverseTransformId)
                  .map(this::transformId)
                  .collect(Collectors.toList());
    }

    @Override
    protected String[] transformPath(String id, String... path) {
        return ObjectArrays.concat(path, entityType);
    }

    private Predicate<String> isTransformed() {
        return id -> id.startsWith(entityType + EntityRepository.ID_SEPARATOR);
    }

    private String reverseTransformId(String id) {
        return id.substring(entityType.length()+1);
    }
}
