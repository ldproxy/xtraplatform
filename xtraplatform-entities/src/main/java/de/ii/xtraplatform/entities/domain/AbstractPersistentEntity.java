/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.entities.app.ChangingDataImpl;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author zahnen
 */
public abstract class AbstractPersistentEntity<T extends EntityData>
    extends AbstractVolatileComposed
    implements PersistentEntity, Reloadable, EntityState, VolatileComposed {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPersistentEntity.class);

  private final ExecutorService executorService;
  private final List<Consumer<PersistentEntity>> reloadListeners;
  private final List<Consumer<EntityState>> stateChangeListeners;
  private final ChangingData changingData;

  /*@ServiceController(
     value = true,
     specification = EntityState.class) // is ignored here, but added by @Entity handler
  */
  private boolean registerState;

  // @ServiceController(value = false) // is ignored here, but added by @Entity handler
  public volatile boolean register;

  private T data;
  private Future<?> startup;
  private EntityState.STATE state;
  private EntityState.STATE previousState;
  private boolean forceReload;

  public AbstractPersistentEntity(
      T data, VolatileRegistry volatileRegistry, String... capabilities) {
    super(volatileRegistry, capabilities);
    this.executorService =
        MoreExecutors.getExitingExecutorService(
            (ThreadPoolExecutor)
                Executors.newFixedThreadPool(
                    1, new ThreadFactoryBuilder().setNameFormat("entity.lifecycle-%d").build()));
    this.reloadListeners = new CopyOnWriteArrayList<>();
    this.stateChangeListeners = new CopyOnWriteArrayList<>();
    this.changingData = new ChangingDataImpl();
    this.data = data;
    this.startup = null;
    this.state = STATE.UNKNOWN;
    this.previousState = STATE.UNKNOWN;
    this.forceReload = false;
    setState(STATE.LOADING);
  }

  @Override
  public T getData() {
    return data;
  }

  @Override
  public ChangingData getChangingData() {
    return changingData;
  }

  // @Property(name = Entity.DATA_KEY) // is ignored here, but added by @Entity handler
  public final void setData(T data, boolean force) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("GOT DATA {}" /*, data*/);
    }
    T previous = this.data;
    this.data = data;
    this.forceReload = force;

    if (force
        || (Objects.nonNull(previous) && !Objects.equals(previous.hashCode(), data.hashCode()))) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("RELOAD DATA {} {}", previous.hashCode(), data.hashCode());
      }
      onReload();
    }
  }

  // @Validate // is ignored here, but added by @EntityComponent stereotype
  public final void onValidate() {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (shouldRegister()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("STARTING {} {} {} {}", getType(), getId(), shouldRegister(), register);
        }
        setState(STATE.LOADING);
        triggerStartup(true, () -> {});
      } else {
        setState(STATE.DISABLED);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("DISABLED {} {} {} {}", getType(), getId(), shouldRegister(), register);
        }
      }
    }
  }

  // @Invalidate // is ignored here, but added by @EntityComponent stereotype
  public final void onInvalidate() {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("STOPPING {} {} {} {}", getType(), getId(), shouldRegister(), register);
      }

      cancelStartup();

      onShutdown();
    }
  }

  // @PostRegistration // is ignored here, but added by @EntityComponent stereotype
  public final void onPostRegistration() {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("STARTED {} {} {} {}", getType(), getId(), shouldRegister(), register);
      }
      if (state == STATE.LOADING || state == STATE.RELOADING) {
        if (shouldRegister() && !register) {
          setState(STATE.DEFECTIVE);
        } else {
          onStarted();
          setState(STATE.ACTIVE);
        }
      }
    }
  }

  // @PostUnregistration // is ignored here, but added by @EntityComponent stereotype
  public final void onPostUnregistration() {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("STOPPED {} {} {} {}", getType(), getId(), shouldRegister(), register);
      }
      onStopped();
      setState(STATE.DISABLED);
    }
  }

  private void onReload() {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("RELOADING {} {} {} {}", getType(), getId(), shouldRegister(), register);
      }
      setState(STATE.RELOADING);

      cancelStartup();

      if (shouldRegister()) {
        triggerStartup(false, this::afterReload);
      } else {
        boolean wasStarted = register;
        this.register = false;
        setState(STATE.DISABLED);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("DISABLED {} {} {} {}", getType(), getId(), shouldRegister(), register);
        }
        if (wasStarted) {
          onInvalidate();
          onPostUnregistration();
          onVolatileStop();
        }
      }
    }
  }

  protected final void doReload() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("MANUAL RELOAD");
    }

    executorService.submit(
        () -> {
          LogContext.put(LogContext.CONTEXT.SERVICE, getId());
          onReload();
        });
  }

  private void afterReload() {
    reloadListeners.forEach(listener -> listener.accept(this));

    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, getId())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("RELOADED {} {} {} {}", getType(), getId(), shouldRegister(), register);
      }

      if (register) {
        onReloaded(forceReload);
        setState(STATE.ACTIVE);
        onVolatileStarted();
      } else {
        setState(STATE.DEFECTIVE);
      }
    }
  }

  private void triggerStartup(boolean wait, Runnable then) {
    this.startup =
        executorService.submit(
            () -> {
              LogContext.put(LogContext.CONTEXT.SERVICE, getId());
              try {
                this.register = onStartup();
              } catch (InterruptedException e) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace(
                      "INTERRUPTED {} {} {} {}", getType(), getId(), shouldRegister(), register, e);
                }
                return;
              } catch (Throwable e) {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace(
                      "DEFECTIVE {} {} {} {}", getType(), getId(), shouldRegister(), register, e);
                }
                this.register = false;
                onStartupFailure(e);
                setState(STATE.DEFECTIVE);
              }
              then.run();
            });

    if (wait) {
      try {
        this.startup.get();
      } catch (InterruptedException | ExecutionException e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              "INTERRUPTED {} {} {} {}", getType(), getId(), shouldRegister(), register, e);
        }
      }
    }
  }

  private void cancelStartup() {
    if (Objects.nonNull(startup)) {
      boolean canceled = startup.cancel(true);
    }
  }

  protected boolean onStartup() throws InterruptedException {
    return true;
  }

  protected void onStarted() {
    onVolatileStarted();
  }

  protected void onReloaded(boolean forceReload) {}

  protected void onShutdown() {}

  protected void onStopped() {}

  protected void onStartupFailure(Throwable throwable) {}

  protected boolean shouldRegister() {
    return getData() != null && getData().getEnabled();
  }

  protected void checkForStartupCancel() throws InterruptedException {
    if (Thread.interrupted()) {
      Thread.currentThread().interrupt();
      throw new InterruptedException();
    }
  }

  @Override
  public <U extends PersistentEntity> void addReloadListener(Class<U> type, Consumer<U> listener) {
    // TODO: only used by ServiceBackgroundTasksImpl, so there is max 1 listener,
    // but under certain circumstances it is added multiple times
    // if (reloadListeners.isEmpty()) {
    this.reloadListeners.add(
        (entity) -> {
          if (type.isAssignableFrom(entity.getClass())) {
            listener.accept(type.cast(entity));
          }
        });
    // }
  }

  @Override
  public String getId() {
    return PersistentEntity.super.getId();
  }

  @Override
  public String getEntityType() {
    return getType();
  }

  @Override
  public EntityState.STATE getEntityState() {
    return state;
  }

  @Override
  public STATE getPreviousState() {
    return previousState;
  }

  public void setState(STATE state) {
    if (this.state != state) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("{}: {} -> {}", getId(), this.state, state);
      }
      this.previousState = this.state;
      this.state = state;
      stateChangeListeners.forEach(listener -> listener.accept(this));
    }
  }

  @Override
  public void addListener(Consumer<EntityState> listener) {
    stateChangeListeners.add(listener);
  }

  @Override
  public String getUniqueKey() {
    return String.format("entities/%s/%s", getType(), getId());
  }
}
