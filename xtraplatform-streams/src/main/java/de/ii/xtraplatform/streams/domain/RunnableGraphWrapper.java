/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.domain;

import de.ii.xtraplatform.base.domain.util.Triple;
import de.ii.xtraplatform.streams.domain.Reactive.StreamContext;
import io.reactivex.rxjava3.core.Flowable;
import java.util.function.Function;
import org.reactivestreams.Subscriber;

public interface RunnableGraphWrapper<T, U> {

  Triple<Flowable<T>, Subscriber<T>, StreamContext<U>> getGraph();

  default Function<Throwable, T> getExceptionHandler() {
    return throwable -> null;
  }
}
