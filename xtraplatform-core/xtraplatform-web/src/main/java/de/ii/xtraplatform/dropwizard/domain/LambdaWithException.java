/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.domain;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** @author zahnen */
public class LambdaWithException {

  @FunctionalInterface
  public interface FunctionWithException<T, R> {
    R apply(T t) throws Throwable;
  }

  @FunctionalInterface
  public interface ConsumerWithException<T, E extends Exception> {
    void apply(T t) throws E;
  }

  @FunctionalInterface
  public interface BiConsumerWithException<T, U, E extends Exception> {
    void apply(T t, U u) throws E;
  }

  @FunctionalInterface
  public interface SupplierWithException<T, E extends Exception> {
    T get() throws E;
  }

  public static <T, R> Function<T, R> mayThrow(FunctionWithException<T, R> fe) {
    return arg -> {
      try {
        return fe.apply(arg);
      } catch (Error | RuntimeException ex) {
        throw ex;
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    };
  }

  public static <T, R, E extends Exception> Consumer<T> consumerMayThrow(
      ConsumerWithException<T, E> ce) {
    return arg -> {
      try {
        ce.apply(arg);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T, U, E extends Exception> BiConsumer<T, U> biConsumerMayThrow(
      BiConsumerWithException<T, U, E> ce) {
    return (arg, arg2) -> {
      try {
        ce.apply(arg, arg2);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    };
  }

  public static <T, E extends Exception> Supplier<T> supplierMayThrow(
      SupplierWithException<T, E> se) {
    return () -> {
      try {
        return se.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
