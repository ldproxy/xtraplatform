package de.ii.xtraplatform.web.domain;

import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;

public interface HttpClient {
  Reactive.Source<byte[]> get(String url);

  InputStream getAsInputStream(String url);

  InputStream postAsInputStream(String url, byte[] body, MediaType mediaType);
}
