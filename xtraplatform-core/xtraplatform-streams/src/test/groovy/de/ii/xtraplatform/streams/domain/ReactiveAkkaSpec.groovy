package de.ii.xtraplatform.streams.domain

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import com.typesafe.config.Config
import de.ii.xtraplatform.streams.app.ReactiveAkka
import de.ii.xtraplatform.streams.domain.Reactive.Source
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced
import de.ii.xtraplatform.streams.domain.Reactive.Transformer
import org.osgi.framework.BundleContext
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CompletionException

class ReactiveAkkaSpec extends Specification {

    @Shared
    ActorSystem system
    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    def setupSpec() {
        reactive = new ReactiveAkka(null, new ActorSystemProvider() {
            @Override
            ActorSystem getActorSystem(BundleContext context) {
                return null
            }

            @Override
            ActorSystem getActorSystem(BundleContext context, Config config) {
                return null
            }

            @Override
            ActorSystem getActorSystem(BundleContext context, Config config, String name) {
                system = ActorSystem.create(name, config)
                return system
            }
        })
        runner = reactive.runner("test")
    }

    def cleanupSpec() {
        runner.close()
        TestKit.shutdownActorSystem(system)
        system = null
    }

    def "success"() {
        given:
        Reactive.Stream<Integer> stream = Source.iterable(1..5)
                .to(SinkReduced.head())
        //Reactive.Stream<Integer> stream = sourceFromRange(1, 5)
        //        .to(reactive.sinks().head())

        when:
        def result = runStream(stream)

        then:
        result == 1
    }

    def "exception fallback transformer"() {
        given:
        Reactive.Stream<Void> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(2))
                .to(sinkThrowingAtIndex(3))

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalArgumentException
    }

    def "exception fallback sink"() {
        given:
        Reactive.Stream<Void> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(3))
                .to(sinkThrowingAtIndex(2))

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalStateException
    }

    def "exception result transformer"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(2))
                .to(sinkThrowingAtIndex(3))
                .withResult([:] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalArgumentException
    }

    def "exception result sink"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(3))
                .to(sinkThrowingAtIndex(2))
                .withResult([:] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalStateException
    }

    def "exception result error handler"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(2))
                .to(SinkReduced.ignore())
                .withResult([:] as Map<String, Object>)
                .handleError((result, throwable) -> {throw new IllegalStateException()})

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalStateException
    }

    def "successful result"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerLogging())
                .to(SinkReduced.ignore())
                .withResult([success: true] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})

        when:
        def result = runStream(stream)

        then:
        result.success == true
        result.error == null
    }

    def "combined result"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerLogging())
                .to(SinkReduced.ignore())
                .withResult([success: true, ids: []] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})
                .handleItem((result, id) -> {
                    result.ids << id
                    return result
                })

        when:
        def result = runStream(stream)

        then:
        result.success == true
        result.error == null
        result.ids == 1..5
    }

    def "exception combined result"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerLogging())
                .to(SinkReduced.ignore())
                .withResult([success: true, ids: []] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})
                .handleItem((result, id) -> {
                    result.ids << id
                    throw new IllegalArgumentException()
                })

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalArgumentException
    }

    def "final result"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerLogging())
                .to(SinkReduced.ignore())
                .withResult([success: false] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})
                .handleEnd(result -> {result.success = true; return result;})

        when:
        def result = runStream(stream)

        then:
        result.success == true
        result.error == null
    }

    def "exception final result"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerLogging())
                .to(SinkReduced.ignore())
                .withResult([success: false] as Map<String, Object>)
                .handleError((result, throwable) -> {result.error = throwable; return result;})
                .handleEnd(result -> {throw new IllegalStateException()})

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalStateException
    }

    static Transformer<Integer, Integer> transformerLogging() {
        return Transformer.peek((Integer i) -> println(i))
    }

    static Transformer<Integer, Integer> transformerThrowingAtIndex(int index) {
        return Transformer.map((Integer i) -> {
            if (i == index) throw new IllegalArgumentException()
            println(i + " transformer")
            return i
        })
        /*return reactive.transformers().of((Integer i) -> {
            if (i == index) throw new IllegalArgumentException()
            println(i + " transformer")
            return i
        })*/
    }

    static SinkReduced<Integer, Void> sinkThrowingAtIndex(int index) {
        return SinkReduced.foreach((Integer i) -> {
            if (i == index) throw new IllegalStateException()
            println(i + " sink")
        })
        /*return reactive.sinks().foreach((Integer i) -> {
            if (i == index) throw new IllegalStateException()
            println(i + " sink")
        })*/
    }

    def runStream(Reactive.Stream<?> stream) {
        def result = stream.on(runner).run().toCompletableFuture().join()
        println(result.toString())
        return result
    }
}
