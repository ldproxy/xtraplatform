package de.ii.xtraplatform.streams.domain;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.function.Procedure;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContextExecutor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamRunner.class);
  public static final int DYNAMIC_CAPACITY = -1;

  private final ActorMaterializer materializer;
  private final String name;
  private final int capacity;
  private final int queueSize;
  private final ConcurrentLinkedQueue<Runnable> queue;
  private final AtomicInteger running;

  public StreamRunner(BundleContext context,
      ActorSystemProvider actorSystemProvider,
      String name) {
    this(context, actorSystemProvider, name, DYNAMIC_CAPACITY, DYNAMIC_CAPACITY);
  }

  public StreamRunner(BundleContext context,
                      ActorSystemProvider actorSystemProvider,
                      String name,
                      int capacity, int queueSize) {
    Config config =
        capacity == DYNAMIC_CAPACITY ? getDefaultConfig(name) : getConfig(name, capacity, capacity);

    if (capacity != 0) {
      ActorSystem system = actorSystemProvider.getActorSystem(context, config, "akka");
      ActorMaterializerSettings settings = ActorMaterializerSettings.create(system)
                                                                    .withDispatcher(getDispatcherName(name));

      this.materializer = ActorMaterializer.create(settings, system);
    } else {
      this.materializer = null;
    }
    this.name = name;
    this.capacity = capacity;
    this.queueSize = queueSize;
    this.queue = new ConcurrentLinkedQueue<>();
    this.running = new AtomicInteger(0);
    //LOGGER.info("RUNNER {} {}", capacity, queueSize);
  }

  public <T, U> CompletionStage<U> run(Source<T, ?> source, Sink<T, CompletionStage<U>> sink) {
    return run(source.toMat(sink, Keep.right()));
    //return source.runWith(sink, materializer);
  }

  //TODO: dynamic capacity
  public <U> CompletionStage<U> run(RunnableGraph<CompletionStage<U>> graph) {
    if (getCapacity() == DYNAMIC_CAPACITY) {
      return graph.run(materializer);
    }
    CompletableFuture<U> completableFuture = new CompletableFuture<>();
    Runnable task = () -> {
      //LOGGER.debug("STARTED STREAM {}-{}", name, running);
      //TODO: throwable case (exceptionally?)
      graph.run(materializer).thenAccept(t -> {
        //LOGGER.debug("FINISHING STREAM {}-{}", name, running);
        completableFuture.complete(t);

        running.decrementAndGet();
        //LOGGER.debug("FINISHED STREAM {}-{}", name, running);

        checkQueue();
      });
    };

    queue.offer(task);
    checkQueue();

    return completableFuture;
  }

  //TODO: can we remove synchronized?
  private synchronized void checkQueue() {
    if (running.get() < queueSize && Objects.nonNull(queue.peek())) {
      running.incrementAndGet();
      //LOGGER.debug("STARTING STREAM {}-{}", name, running);
      queue.poll().run();
    }
  }

  public <T> CompletionStage<Done> runForeach(Source<T, ?> source, Procedure<T> procedure) {
    return run(source, Sink.foreach(procedure));
    //return source.runForeach(procedure, materializer);
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
