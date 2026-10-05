package core;

import config.EnvironmentConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

//Request/response logging, controlled by http.log in the properties file (or -Dhttp.log=...):
//  failures (default) - collect each test's HTTP traffic and log it only if the test fails
//  all                - log every request and response as it happens (one log entry per call)
//  none               - no HTTP logging
public final class HttpLog {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLog.class);

    //One buffer per thread, so parallel tests never mix their logs
    private static final ThreadLocal<ByteArrayOutputStream> BUFFER = ThreadLocal.withInitial(ByteArrayOutputStream::new);
    private static final PrintStream BUFFERED = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
            BUFFER.get().write(b);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            BUFFER.get().write(bytes, offset, length);
        }
    }, true, StandardCharsets.UTF_8);

    //Runs first and wraps the logging filters, so the request and its response become one log entry
    private static final Filter LOG_EACH_CALL = (request, response, context) -> {
        try {
            return context.next(request, response);
        } finally {
            LOG.info("HTTP call:\n{}", drain().stripTrailing());
        }
    };

    private HttpLog() {
    }

    public static List<Filter> filters() {
        return switch (mode()) {
            case "all" -> List.of(LOG_EACH_CALL, new RequestLoggingFilter(BUFFERED), new ResponseLoggingFilter(BUFFERED));
            case "none" -> List.of();
            case "failures" -> List.of(new RequestLoggingFilter(BUFFERED), new ResponseLoggingFilter(BUFFERED));
            default -> throw new IllegalStateException("http.log must be failures, all or none but was: " + mode());
        };
    }

    public static boolean isBuffered() {
        return mode().equals("failures");
    }

    //Called by HttpLogListener: start each test with an empty buffer
    public static void clear() {
        BUFFER.get().reset();
    }

    //Called by HttpLogListener when a test fails: the traffic leading up to the failure
    public static String drain() {
        String log = BUFFER.get().toString(StandardCharsets.UTF_8);
        clear();
        return log;
    }

    private static String mode() {
        return EnvironmentConfig.get("http.log").toLowerCase();
    }
}
