/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

import de.ii.xtraplatform.store.legacy.Transaction;
import de.ii.xtraplatform.store.legacy.TransactionSupport;
import de.ii.xtraplatform.store.legacy.WriteTransaction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** @author fischer */
public class ResourceCache<T extends Resource> implements TransactionSupport<T> {

  private final boolean fullCache;
  Map<String, T> resourceCache;
  List<String> resourceIdCache;

  public ResourceCache(boolean fullCache) {
    this.fullCache = fullCache;
    this.resourceIdCache = new CopyOnWriteArrayList<>();
    if (this.fullCache) {
      resourceCache = new ConcurrentHashMap<>();
    }
  }

  public void put(String id, T resource) {
    if (!this.resourceIdCache.contains(id)) {
      this.resourceIdCache.add(id);
    }
    if (this.fullCache) {
      this.resourceCache.put(id, resource);
    }
  }

  public void add(List<String> ids) {
    this.resourceIdCache = ids;
  }

  public void add(Map<String, T> resourceCache) {
    this.resourceCache = resourceCache;
  }

  public T get(String id) {
    if (this.fullCache) {
      return resourceCache.get(id);
    }
    // TODO ?
    return null;
  }

  public boolean hasResource(String id) {
    if (fullCache) {
      return this.resourceCache.containsKey(id);
    } else {
      return this.resourceIdCache.contains(id);
    }
  }

  public List<String> getResourceIds() {
    return this.resourceIdCache;
  }

  public void remove(String id) {
    this.resourceIdCache.remove(id);
    if (this.fullCache) {
      this.resourceCache.remove(id);
    }
  }

  @Override
  public Transaction openDeleteTransaction(String key) {
    return new DeleteCacheTransaction<>(this, key);
  }

  @Override
  public WriteTransaction<T> openWriteTransaction(String key) {
    return new WriteCacheTransaction<>(this, key);
  }
}
