package de.ii.xtraplatform.runtime.domain;

import org.slf4j.MDC;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zahnen
 */
public class Logging {

    public enum CONTEXT {
        SERVICE,
        REQUEST
    }

    public enum MARKER {
        SQL
    }

    public static boolean has(CONTEXT context) {
        return Objects.nonNull(MDC.get(context.name()));
    }

    public static String get(CONTEXT context) {
        return MDC.get(context.name());
    }

    public static void put(CONTEXT context, String value) {
        MDC.put(context.name(), value);
    }

    public static void remove(CONTEXT context) {
        MDC.remove(context.name());
    }

    public static MDC.MDCCloseable putCloseable(CONTEXT context, String value) {
        return MDC.putCloseable(context.name(), value);
    }

    public static Runnable withMdc(Runnable runnable) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();

        if (Objects.nonNull(mdc)) {
            return () -> {
                MDC.setContextMap(mdc);
                runnable.run();
            };
        }
        return runnable;
    }

    public static <U> Callable<U> withMdc(Callable<U> callable) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();

        if (Objects.nonNull(mdc)) {
            return () -> {
                MDC.setContextMap(mdc);
                return callable.call();
            };
        }
        return callable;
    }

    /**
     * Generate a random UUID v4 that will perform reasonably when used by
     * multiple threads under load.
     *
     * @see <a href="https://github.com/Netflix/netflix-commons/blob/v0.3.0/netflix-commons-util/src/main/java/com/netflix/util/concurrent/ConcurrentUUIDFactory.java">ConcurrentUUIDFactory</a>
     * @return random UUID
     */
    public static UUID generateRandomUuid() {
        final Random rnd = ThreadLocalRandom.current();
        long mostSig  = rnd.nextLong();
        long leastSig = rnd.nextLong();

        // Identify this as a version 4 UUID, that is one based on a random value.
        mostSig &= 0xffffffffffff0fffL;
        mostSig |= 0x0000000000004000L;

        // Set the variant identifier as specified for version 4 UUID values.  The two
        // high order bits of the lower word are required to be one and zero, respectively.
        leastSig &= 0x3fffffffffffffffL;
        leastSig |= 0x8000000000000000L;

        return new UUID(mostSig, leastSig);
    }
}
