package utilities;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

//Repeats an action until its result meets a condition, or fails after a timeout.
//Use this for anything asynchronous (Kafka events, workflow steps) instead of fixed sleeps
public final class Poller {

    private static final Duration INTERVAL = Duration.ofSeconds(2);

    private Poller() {
    }

    public static <T> T waitUntil(String description, Supplier<T> action, Predicate<T> condition, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        T result = action.get();
        while (!condition.test(result)) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Timed out after " + timeout.toSeconds() + "s waiting for "
                        + description + ". Last result: " + result);
            }
            sleep();
            result = action.get();
        }
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
