/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.domain;

import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;

public final class LogContextStream {

  private LogContextStream() {}

  // TODO: apply to first flowable
  public static <T, U> Source<T> withMdc(Source<T> source) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return source.via(
          Transformer.map(
              t -> {
                MDC.setContextMap(mdc);
                return t;
              }));
    }
    return source;
  }
}
