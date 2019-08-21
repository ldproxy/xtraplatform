package de.ii.xtraplatform.entity.api.maptobuilder.encoding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.entity.api.maptobuilder.ImmutableValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import org.immutables.encode.Encoding;

import java.util.LinkedHashMap;
import java.util.Map;

@Encoding
class ValueBuilderMapEncoding<T extends ValueInstance, U extends ValueBuilder<T>> {
    @Encoding.Impl
    private ValueBuilderMap<T, U> field;

    @Encoding.Expose
    ValueBuilderMap<T, U> getImmutableTable() {
        return field; // <-- this is how our accessor would be implemented
    }

    @Encoding.Builder  // <-- put annotation
    static class Builder<T extends ValueInstance, U extends ValueBuilder<T>> { // <-- copy type parameters from the encoding

        //private ImmutableValueOrBuilderMap.Builder<T, U> buildValue = new ImmutableValueOrBuilderMap.Builder<T, U>();
        private Map<String, U> builderMap = new LinkedHashMap<>();

        @JsonIgnore
        @Encoding.Init // <-- specify builder initializer method
        @Encoding.Copy // <-- marks it as "canonical" copy method
        public void set(Map<String, T> values) {
            //this.buildValue = new ImmutableValueOrBuilderMap.Builder<T, U>();//.from(value);
            //values.forEach(this::put);
            this.builderMap = new LinkedHashMap<>();
            values.forEach(this::put);
        }

        @JsonProperty
        @Encoding.Naming(value = "get*")
        public Map<String, U> get() {
            //return buildValue.build().getBuilders();
            return builderMap;
        }

        @Encoding.Init
        @Encoding.Naming(standard = Encoding.StandardNaming.PUT, depluralize = true)
        void put(String key, T value) {
            // TODO
            //buildValue.putBuilders(key, value.toBuilder());
            builderMap.put(key, value.toBuilder());
        }

        @Encoding.Init
        @Encoding.Naming(standard = Encoding.StandardNaming.PUT, depluralize = true)
        void putBuilder(String key, U builder) {
            // here, table builder handles checks for us
            //buildValue.putBuilders(key, builder);
            builderMap.put(key, builder);
        }

        @Encoding.Init
        @Encoding.Naming(standard = Encoding.StandardNaming.PUT_ALL, depluralize = true)
        void putAll(Map<String, T> values) {
            // TODO
            values.forEach(this::put);
        }

        @JsonProperty
        @Encoding.Init
        @Encoding.Naming(depluralize = true, value = "set*")
        void putAllBuilders(Map<String, U> builders) {
            // here, table builder handles checks for us
            //buildValue.putAllBuilders(builders);
            builderMap.putAll(builders);
        }

        @Encoding.Build
        ValueBuilderMap<T, U> build() {
            //return buildValue.build();
            return new ImmutableValueBuilderMap.Builder<T, U>().builders(builderMap)
                                                               .build();
        }
    }
}
