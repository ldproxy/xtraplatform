/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class JoinableStreamingOutput implements StreamingOutput {

  private final StreamingOutput delegate;
  private final CompletableFuture<Void> completableFuture;

  public JoinableStreamingOutput(StreamingOutput delegate) {
    this.delegate = delegate;
    this.completableFuture = new CompletableFuture<>();
  }

  public void whenComplete(Consumer<Throwable> onComplete) {
    completableFuture.whenComplete(
        (result, throwable) -> {
          onComplete.accept(throwable);
        });
  }

  @Override
  @SuppressWarnings({"PMD.AvoidCatchingGenericException"})
  public void write(OutputStream outputStream) throws IOException {
    try {
      delegate.write(outputStream);
    } catch (Throwable throwable) {
      completableFuture.completeExceptionally(throwable);
      throw throwable;
    } finally {
      completableFuture.complete(null);
    }
  }
}
