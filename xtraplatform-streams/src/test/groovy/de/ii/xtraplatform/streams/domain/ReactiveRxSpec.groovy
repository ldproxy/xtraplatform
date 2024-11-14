package de.ii.xtraplatform.streams.domain


import de.ii.xtraplatform.streams.app.ReactiveRx
import de.ii.xtraplatform.streams.domain.Reactive.Sink
import de.ii.xtraplatform.streams.domain.Reactive.Source
import de.ii.xtraplatform.streams.domain.Reactive.Transformer
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CompletionException

class ReactiveRxSpec extends Specification {

    @Shared
    Reactive reactive
    @Shared
    Reactive.Runner runner

    def setupSpec() {
        reactive = new ReactiveRx()
        runner = reactive.runner("test")
    }

    def cleanupSpec() {
        runner.close()
    }

    def "success"() {
        given:
        Reactive.Stream<Integer> stream = Source.iterable(1..5)
                .to(Sink.head())

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
                .handleError((result, throwable) -> { result.error = throwable; return result; })

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
                .handleError((result, throwable) -> { result.error = throwable; return result; })

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalStateException
    }

    def "exception result error handler"() {
        given:
        Reactive.Stream<Map<String, Object>> stream = Source.iterable(1..5)
                .via(transformerThrowingAtIndex(2))
                .to(Sink.ignore())
                .withResult([:] as Map<String, Object>)
                .handleError((result, throwable) -> { throw new IllegalStateException() })

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
                .to(Sink.ignore())
                .withResult([success: true] as Map<String, Object>)
                .handleError((result, throwable) -> { result.error = throwable; return result; })

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
                .to(Sink.ignore())
                .withResult([success: true, ids: []] as Map<String, Object>)
                .handleError((result, throwable) -> { result.error = throwable; return result; })
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
                .to(Sink.ignore())
                .withResult([success: true, ids: []] as Map<String, Object>)
                .handleError((result, throwable) -> { result.error = throwable;
                    result.success = false; return result; })
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
                .to(Sink.ignore())
                .withResult([success: false] as Map<String, Object>)
                .handleError((result, throwable) -> { result.error = throwable; return result; })
                .handleEnd(result -> { result.success = true; return result; })

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
                .to(Sink.ignore())
                .withResult([success: false] as Map<String, Object>)
                .handleError((result, throwable) -> { result.error = throwable; return result; })
                .handleEnd(result -> { throw new IllegalStateException() })

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

    static Sink<Integer> sinkThrowingAtIndex(int index) {
        return Sink.foreach((Integer i) -> {
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
