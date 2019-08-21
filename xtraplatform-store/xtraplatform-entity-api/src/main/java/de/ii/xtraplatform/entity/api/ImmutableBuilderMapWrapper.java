package de.ii.xtraplatform.entity.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class ImmutableBuilderMapWrapper<T,U extends ValueBuilder<T>> {

    // internal map of builders
    @JsonIgnore
    protected abstract Map<String, U> getMapBuilders();

    // map of instances
    @Value.Derived
    public Map<String, T> getMap() {
        return getMapBuilders()
                .entrySet()
                .stream()
                .map(stringTestEntry -> new AbstractMap.SimpleEntry<>(stringTestEntry.getKey(), stringTestEntry.getValue().build()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected interface Builder<T,U extends ValueBuilder<T>> {

        Builder mapBuilders(Map<String, ? extends U> entries);
        Builder putMapBuilders(String key, U value);
        ImmutableBuilderMapWrapper<T,U> build();

        // for jackson mergeable deserialization
        // return observable map that intercepts put and feeds the given builders back to the derived builder
        default Map<String, U> getMap() {
            Map<String, U> builderMap = build().getMapBuilders();
            mapBuilders(builderMap);
            Map<String, U> testMap = new ObservableMap<T,U>(builderMap, this::putMapBuilders);
            return testMap;

        }

        // just has to be there for jackson mergeable detection, never used
        default Builder setMap(Map<String, U> entries) {
            return this;
        }
    }

    private static class ObservableMap<T,U extends ValueBuilder<T>> extends HashMap<String, U> implements Map<String, U> {
        private final BiConsumer<String, U> putter;

        public ObservableMap(Map<String, U> list, BiConsumer<String, U> putter) {
            super(list);
            this.putter = putter;
        }

        @Override
        public U put(String key, U value) {
            putter.accept(key, value);
            return super.put(key, value);
        }
    }

}
