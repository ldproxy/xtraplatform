package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EntityStoreDecorator<T extends EntityData, U extends T> extends EntityDataStore<U> {

    EntityDataStore<T> getDecorated();

    String[] transformPath(String... path);

    @Override
    default List<String> ids(String... path) {
        return getDecorated().ids(transformPath(path));
    }

    @Override
    default boolean has(String id, String... path) {
        return getDecorated().has(id, transformPath(path));
    }

    @Override
    default U get(String id, String... path) {
        return (U) getDecorated().get(id, transformPath(path));
    }

    @Override
    default CompletableFuture<U> put(String id, U value, String... path) {
        return (CompletableFuture<U>) getDecorated().put(id, value, transformPath(path));
    }

    @Override
    default CompletableFuture<Boolean> delete(String id, String... path) {
        return getDecorated().delete(id, transformPath(path));
    }

    @Override
    default CompletableFuture<U> patch(String id, Map<String, Object> partialData, String... path) {
        return (CompletableFuture<U>) getDecorated().patch(id, partialData, transformPath(path));
    }

    @Override
    default List<Identifier> identifiers(String... path) {
        return getDecorated().identifiers(transformPath(path));
    }

    //TODO: transformPath
    @Override
    default boolean has(Identifier identifier) {
        return getDecorated().has(identifier);
    }

    //TODO
    @Override
    default U get(Identifier identifier) {
        return null;// getDecorated().get(identifier);
    }

    //TODO
    @Override
    default CompletableFuture<U> put(Identifier identifier, U value) {
        return null;//getDecorated().patch(identifier, value);
    }

    @Override
    default <U1 extends U> EntityDataStore<U1> forType(Class<U1> type) {
        throw new IllegalArgumentException();
    }
}
