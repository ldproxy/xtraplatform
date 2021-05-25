package de.ii.xtraplatform.streams.domain

import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.testkit.javadsl.TestKit
import com.typesafe.config.Config
import org.osgi.framework.BundleContext
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CompletionException

class ReactiveStreamSpec extends Specification {

    @Shared
    ActorSystem system
    @Shared
    StreamRunner streamRunner

    def setupSpec() {
        streamRunner = new StreamRunner(null, new ActorSystemProvider() {
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
        }, "test")
    }

    def cleanupSpec() {
        streamRunner.close()
        TestKit.shutdownActorSystem(system)
        system = null
    }

    static ReactiveStream.Source<Integer> sourceFromRange(int start, int end) {
        return ReactiveStream.Source.of(Source.range(start, end))
    }

    static ReactiveStream.Processor<Integer, Integer> processorThrowingAtIndex(int index) {
        return ReactiveStream.Processor.of((Integer i) -> {
            if (i == index) throw new IllegalArgumentException()
            println(i)
            return i
        })
    }

    static ReactiveStream.Sink<Integer> sinkThrowingAtIndex(int index) {
        return ReactiveStream.Sink.of((Integer i) -> {
            if (i == index) throw new IllegalStateException()
            println(i)
        })
    }

    def "exception fallback processor"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(2))
                .sink(sinkThrowingAtIndex(3))
                .build()

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalArgumentException
    }

    def "exception fallback sink"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(3))
                .sink(sinkThrowingAtIndex(2))
                .build()

        when:
        runStream(stream)

        then:
        def e = thrown(CompletionException)
        e.cause instanceof IllegalStateException
    }

    def "exception result processor"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(2))
                .exceptionHandler((result, throwable) -> ["error": throwable])
                .build()

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalArgumentException
    }

    def "exception result sink"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(3))
                .sink(sinkThrowingAtIndex(2))
                .exceptionHandler((result, throwable) -> ["error": throwable])
                .build()

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalStateException
    }

    def "successful result"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(6))
                .emptyResult(["success": true])
                .exceptionHandler((result, throwable) -> ["error": throwable])
                .build()

        when:
        def result = runStream(stream)

        then:
        result.success == true
        result.error == null
    }

    def "combined result"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(6))
                .emptyResult(["success": true, ids: []])
                .exceptionHandler((result, throwable) -> ["error": throwable])
                .resultCombiner((result, id) -> {
                            result.ids << id
                            return result
                        })
                .build()

        when:
        def result = runStream(stream)

        then:
        result.success == true
        result.error == null
        result.ids == 1..5
    }

    def "exception combined result"() {
        given:
        ReactiveStream<Integer, Integer, Map<String, Object>, Map<String, Object>> stream = ImmutableReactiveStream.<Integer, Integer, Map<String, Object>, Map<String, Object>> builder()
                .source(sourceFromRange(1, 5))
                .processor(processorThrowingAtIndex(6))
                .emptyResult(["success": true, ids: []])
                .exceptionHandler((result, throwable) -> ["error": throwable])
                .resultCombiner((result, ids) -> {
                            result.ids << ids
                            throw new IllegalArgumentException()
                        })
                .build()

        when:
        def result = runStream(stream)

        then:
        result.error instanceof IllegalArgumentException
    }

    Map<String, Object> runStream(ReactiveStream<?, ?, Map<String, Object>, Map<String, Object>> stream) {
        def result = streamRunner.run(stream).toCompletableFuture().join()
        println(result.toString())
        return result
    }
}
