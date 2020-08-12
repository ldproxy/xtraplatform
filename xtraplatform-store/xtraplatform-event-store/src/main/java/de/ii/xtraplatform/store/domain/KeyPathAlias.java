package de.ii.xtraplatform.store.domain;

import java.util.Map;

public interface KeyPathAlias {
    Map<String, Object> wrapMap(Map<String, Object> value);
}
