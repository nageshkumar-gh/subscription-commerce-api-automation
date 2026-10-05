package utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

//Repeats an action until its result meets a condition, or fails after a timeout.
//Use this for anything asynchronous (Kafka events, workflow steps) instead of fixed sleeps
public final class Poller {

    private static final Logger LOG = LoggerFactory.getLogger(Poller.class);
    private static final Duration INTERVAL = Duration.ofSeconds(2);

    private Poller() {
    }

    public static <T> T waitUntil(String description, Supplier<T> action, Predicate<T> condition, Duration timeout) {
        Instant start = Instant.now();
        Instant deadline = start.plus(timeout);
        LOG.debug("Waiting up to {}s for {}", timeout.toSeconds(), description);
        T result = action.get();
        int attempts = 1;
        while (!condition.test(result)) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Timed out after " + timeout.toSeconds() + "s waiting for "
                        + description + ". Last result: " + result);
            }
            sleep();
            result = action.get();
            attempts++;
        }
        LOG.debug("Done waiting for {} after {} attempt(s), {} ms", description, attempts, Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private static void sleep() {
        try {
            Thread.sleep(INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while polling", e);
        }
    }
}
