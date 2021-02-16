package de.ii.xtraplatform.akka.http;

import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.model.ContentType.NonBinary;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.Authorization;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import scala.util.Try;

public class HttpHostClientAkkaBasicAuth extends HttpHostClientAkka {

  private final String username;
  private final String password;

  HttpHostClientAkkaBasicAuth(ActorMaterializer materializer,
      Flow<Pair<HttpRequest, Object>, Pair<Try<HttpResponse>, Object>, HostConnectionPool> connectionPool,
      String username, String password) {
    super(materializer, connectionPool);
    this.username = username;
    this.password = password;
  }

  @Override
  protected HttpRequest createHttpGet(String url) {
    return super.createHttpGet(url).addHeader(Authorization.basic(username, password));
  }

  @Override
  protected HttpRequest createHttpPost(String url, byte[] body, NonBinary contentType) {
    return super.createHttpPost(url, body, contentType)
        .addHeader(Authorization.basic(username, password));
  }
}
