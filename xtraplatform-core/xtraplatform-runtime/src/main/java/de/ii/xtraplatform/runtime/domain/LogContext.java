/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime.domain;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;

/** @author zahnen */
public class LogContext {

  public enum CONTEXT {
    SERVICE,
    REQUEST
  }

  public enum MARKER implements MyMarker {
    DEV,
    SQL,
    SQL_RESULT,
    STACKTRACE,
    DUMP;

    @Override
    public String toString() {
      return "#" + super.toString();
    }

    @Override
    public String getName() {
      return toString();
    }
  }

  public static boolean has(CONTEXT context) {
    return Objects.nonNull(MDC.get(context.name()));
  }

  public static String get(CONTEXT context) {
    return MDC.get(context.name());
  }

  public static void put(CONTEXT context, String value) {
    MDC.put(context.name(), value);
  }

  public static void remove(CONTEXT context) {
    MDC.remove(context.name());
  }

  public static MDC.MDCCloseable putCloseable(CONTEXT context, String value) {
    return MDC.putCloseable(context.name(), value);
  }

  public static Runnable withMdc(Runnable runnable) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return () -> {
        MDC.setContextMap(mdc);
        runnable.run();
      };
    }
    return runnable;
  }

  public static <U> Callable<U> withMdc(Callable<U> callable) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return () -> {
        MDC.setContextMap(mdc);
        return callable.call();
      };
    }
    return callable;
  }

  public static <U> Consumer<U> withMdc(Consumer<U> consumer) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return (u) -> {
        MDC.setContextMap(mdc);
        consumer.accept(u);
      };
    }
    return consumer;
  }

  public static <T, U, V> BiFunction<T, U, V> withMdc(BiFunction<T, U, V> biFunction) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return (t, u) -> {
        MDC.setContextMap(mdc);
        return biFunction.apply(t, u);
      };
    }
    return biFunction;
  }

  public static void error(
      String messagePrefix, Throwable throwable, Logger logger, String... messagePrefixArgs) {
    String[] strings = Arrays.copyOf(messagePrefixArgs, messagePrefixArgs.length + 1);
    strings[messagePrefixArgs.length] = throwable.getMessage();

    logger.error(messagePrefix + ": {}", (Object[]) strings);
    if (logger.isDebugEnabled()) {
      logger.debug("Stacktrace:", throwable);
    }
  }

  /**
   * Generate a random UUID v4 that will perform reasonably when used by multiple threads under
   * load.
   *
   * @see <a
   *     href="https://github.com/Netflix/netflix-commons/blob/v0.3.0/netflix-commons-util/src/main/java/com/netflix/util/concurrent/ConcurrentUUIDFactory.java">ConcurrentUUIDFactory</a>
   * @return random UUID
   */
  public static UUID generateRandomUuid() {
    final Random rnd = ThreadLocalRandom.current();
    long mostSig = rnd.nextLong();
    long leastSig = rnd.nextLong();

    // Identify this as a version 4 UUID, that is one based on a random value.
    mostSig &= 0xffffffffffff0fffL;
    mostSig |= 0x0000000000004000L;

    // Set the variant identifier as specified for version 4 UUID values.  The two
    // high order bits of the lower word are required to be one and zero, respectively.
    leastSig &= 0x3fffffffffffffffL;
    leastSig |= 0x8000000000000000L;

    return new UUID(mostSig, leastSig);
  }

  private interface MyMarker extends Marker {

    @Override
    default void add(Marker reference) {}

    @Override
    default boolean remove(Marker reference) {
      return false;
    }

    @Override
    default boolean hasChildren() {
      return false;
    }

    @Override
    default boolean hasReferences() {
      return false;
    }

    @Override
    default Iterator<Marker> iterator() {
      return null;
    }

    @Override
    default boolean contains(Marker other) {
      return false;
    }

    @Override
    default boolean contains(String name) {
      return false;
    }
  }
}
