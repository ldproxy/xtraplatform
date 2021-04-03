package de.ii.xtraplatform.store.domain.entities;

import java.util.function.Consumer;

public interface Reloadable {
  <T extends PersistentEntity> void addReloadListener(Class<T> type, Consumer<T> listener);
}
