package de.ii.xtraplatform.streams.domain;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.function.Procedure;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.osgi.framework.BundleContext;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.CompletionStage;

public class StreamRunner {

  public static final int DYNAMIC_CAPACITY = -1;

  private final ActorMaterializer materializer;
  private final int capacity;

  public StreamRunner(BundleContext context,
      ActorSystemProvider actorSystemProvider,
      String name) {
    this(context, actorSystemProvider, name, DYNAMIC_CAPACITY);
  }

  public StreamRunner(BundleContext context,
      ActorSystemProvider actorSystemProvider,
      String name,
      int capacity) {
    Config config =
        capacity == DYNAMIC_CAPACITY ? getDefaultConfig(name) : getConfig(name, capacity, capacity);

    ActorSystem system = actorSystemProvider.getActorSystem(context, config, "akka");
    ActorMaterializerSettings settings = ActorMaterializerSettings.create(system)
        .withDispatcher(getDispatcherName(name));

    this.materializer = ActorMaterializer.create(settings, system);
    this.capacity = capacity;
  }

  public <T, U> CompletionStage<U> run(Source<T, ?> source, Sink<T, CompletionStage<U>> sink) {
    return source.runWith(sink, materializer);
  }

  public <U> CompletionStage<U> run(RunnableGraph<CompletionStage<U>> graph) {
    return graph.run(materializer);
  }

  public <T> CompletionStage<Done> runForeach(Source<T, ?> source, Procedure<T> procedure) {
    return source.runForeach(procedure, materializer);
  }

  public ExecutionContextExecutor getDispatcher() {
    return materializer.system().dispatcher();
  }

  public int getCapacity() {
    return capacity;
  }

  private Config getDefaultConfig(String name) {
    return getConfig(name, 8, 64);
  }

  private Config getConfig(String name, int parallelismMin, int parallelismMax) {
    return ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
        .put("akka.stdout-loglevel", "OFF")
        .put("akka.loglevel", "INFO")
        .put("akka.loggers", ImmutableList.of("akka.event.slf4j.Slf4jLogger"))
        .put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter")
        //.put("akka.log-config-on-start", true)
        .put(String.format("%s.type", getDispatcherName(name)), "Dispatcher")
        //.put(String.format("%s.executor", getDispatcherName(name)), "fork-join-executor")
        .put(String.format("%s.executor", getDispatcherName(name)), "de.ii.xtraplatform.streams.app.StreamExecutorServiceConfigurator")
        .put(String.format("%s.fork-join-executor.parallelism-min", getDispatcherName(name)),
            parallelismMin)
        .put(String.format("%s.fork-join-executor.parallelism-factor", getDispatcherName(name)),
            1.0)
        .put(String.format("%s.fork-join-executor.parallelism-max", getDispatcherName(name)),
            parallelismMax)
        .put(String.format("%s.fork-join-executor.task-peeking-mode", getDispatcherName(name)),
            "FIFO")
        .build());
  }

  private String getDispatcherName(String name) {
    return String.format("stream.%s", name);
  }
}
