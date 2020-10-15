package de.ii.xtraplatform.streams.domain;

import akka.stream.javadsl.RunnableGraph;

@FunctionalInterface
public interface RunnableGraphWithMdc<T> {
    RunnableGraph<T> getGraph();
}
