package de.ii.xtraplatform.dropwizard.app;

public interface WebServer {

  String getUrl();

  public enum StartStopAction {
    START,
    STOP,
    RESTART
  }
}
