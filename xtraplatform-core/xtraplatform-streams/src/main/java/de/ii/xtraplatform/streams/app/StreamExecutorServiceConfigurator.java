package de.ii.xtraplatform.streams.app;

import akka.dispatch.DispatcherPrerequisites;
import akka.dispatch.ExecutorServiceConfigurator;
import akka.dispatch.ExecutorServiceFactory;
import akka.dispatch.ForkJoinExecutorConfigurator;
import akka.dispatch.LoadMetrics;
import akka.dispatch.MonitorableThreadFactory;
import akka.dispatch.forkjoin.ForkJoinPool;
import akka.dispatch.forkjoin.ForkJoinWorkerThread;
import com.typesafe.config.Config;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.ii.xtraplatform.runtime.domain.Logging.withMdc;

/**
 * @author zahnen
 */
public class StreamExecutorServiceConfigurator extends ExecutorServiceConfigurator {

   private final ForkJoinExecutorConfigurator delegate;

    public StreamExecutorServiceConfigurator(Config config, DispatcherPrerequisites prerequisites) {
        super(config, prerequisites);
        this.delegate = new ForkJoinExecutorConfigurator(config.getConfig("fork-join-executor"), prerequisites);
    }


    @Override
    public ExecutorServiceFactory createExecutorServiceFactory(String id, ThreadFactory threadFactory) {
        if (!(threadFactory instanceof MonitorableThreadFactory)) {
            throw new IllegalStateException(
                    "The prerequisites for the StreamExecutorServiceConfigurator is a MonitorableThreadFactory!");
        }

        MyThreadFactory tf = new MyThreadFactory(id, (MonitorableThreadFactory) threadFactory);

        ExecutorServiceFactory executorServiceFactory = delegate.createExecutorServiceFactory(id, tf);

        return new MyExecutorServiceFactory(executorServiceFactory);
    }

    class MyForkJoinPool implements ExecutorService, LoadMetrics {

        private final ForkJoinExecutorConfigurator.AkkaForkJoinPool delegate;

        MyForkJoinPool(ForkJoinExecutorConfigurator.AkkaForkJoinPool delegate) {
            this.delegate = delegate;
        }


        @Override
        public void execute(Runnable task) {
            delegate.execute(withMdc(task));
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
            return delegate.awaitTermination(l, timeUnit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable) {
            return delegate.submit(callable);
        }

        @Override
        public <T> Future<T> submit(Runnable runnable, T t) {
            return delegate.submit(runnable, t);
        }

        @Override
        public Future<?> submit(Runnable runnable) {
            return delegate.submit(runnable);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
            return delegate.invokeAll(collection);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
            return delegate.invokeAll(collection, l, timeUnit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(collection);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(collection, l, timeUnit);
        }

        @Override
        public boolean atFullThrottle() {
            return delegate.atFullThrottle();
        }
    }

    class MyExecutorServiceFactory implements ExecutorServiceFactory {

        private final ExecutorServiceFactory delegate;

        MyExecutorServiceFactory(ExecutorServiceFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public ExecutorService createExecutorService() {
            ForkJoinExecutorConfigurator.AkkaForkJoinPool akkaForkJoinPool = (ForkJoinExecutorConfigurator.AkkaForkJoinPool) delegate.createExecutorService();

            return new MyForkJoinPool(akkaForkJoinPool);
        }
    }

    class MyThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory, ThreadFactory {

        private final String name;
        private final MonitorableThreadFactory delegate;

        public MyThreadFactory(String name, MonitorableThreadFactory delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread t = delegate.newThread(pool);
            t.setName(name + "-" + delegate.counter());

            return t;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = delegate.newThread(runnable);
            t.setName(name + "-" + delegate.counter());

            return t;
        }
    }
}
