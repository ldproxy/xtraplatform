package de.ii.xtraplatform.entity.api;

/**
 * @author zahnen
 */
public interface EntityDataGenerator<T extends AbstractEntityData> {
    Class<T> getType();
    T generate(T partialData);
}
