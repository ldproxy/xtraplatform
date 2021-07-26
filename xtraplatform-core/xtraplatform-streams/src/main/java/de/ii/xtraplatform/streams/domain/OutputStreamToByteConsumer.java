package de.ii.xtraplatform.streams.domain;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class OutputStreamToByteConsumer extends OutputStream {

  private Consumer<byte[]> byteConsumer;

  public OutputStreamToByteConsumer(Consumer<byte[]> byteConsumer) {
    this.byteConsumer = byteConsumer;
  }

  public OutputStreamToByteConsumer() {
  }

  public void setByteConsumer(Consumer<byte[]> byteConsumer) {
    this.byteConsumer = byteConsumer;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    Objects.requireNonNull(byteConsumer, "OutputStream needs byteConsumer");
    Objects.checkFromIndexSize(off, len, b.length);

    byteConsumer.accept(Arrays.copyOfRange(b, off, len));
  }

  @Override
  public void write(int i) throws IOException {
    throw new UnsupportedOperationException();
  }
}
