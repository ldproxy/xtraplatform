package de.ii.xtraplatform.entities.domain.maptobuilder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.Map;

@Value.Immutable
//@JsonSerialize(as = Map.class)
@JsonDeserialize(as = ImmutableValueBuilderMap.class, builder = ImmutableValueBuilderMap.Builder.class)
public abstract class ValueBuilderMap<T extends ValueInstance, U extends ValueBuilder<T>> extends ForwardingMap<String, T> {

    static abstract class Builder {
    }

    @Value.Derived
    Map<String, T> getDelegate() {
        return /*Stream.concat(
                getInstances().entrySet()
                              .stream(),
                */getBuilders().entrySet()
                             .stream()
                             .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                       .build()))
        //)
                     .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    abstract Map<String, U> getBuilders();

    //abstract Map<String, T> getInstances();

    @Override
    protected Map<String, T> delegate() {
        return getDelegate();
    }
}
