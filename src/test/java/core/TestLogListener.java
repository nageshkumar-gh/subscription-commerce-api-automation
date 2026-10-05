package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.stream.Collectors;

//Logs every test's start, result and duration, plus a summary. Registered in META-INF/services/org.testng.ITestNGListener
public class TestLogListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TestLogListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("START {}", label(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS  {} ({} ms)", label(result), duration(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("FAIL  {} ({} ms): {}", label(result), duration(result), reason(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIP  {}: {}", label(result), reason(result));
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("Finished '{}': {} passed, {} failed, {} skipped",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    //e.g. "CustomerLoginTest.loginWithValidCredentials - CUS-LOG-01: valid credentials ..." plus data-provider values
    private static String label(ITestResult result) {
        String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        String parameters = result.getParameters().length == 0 ? "" : Arrays.stream(result.getParameters())
                .map(String::valueOf)
                .collect(Collectors.joining(", ", " [", "]"));
        return name + parameters + (description == null ? "" : " - " + description);
    }

    private static long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }

    private static String reason(ITestResult result) {
        Throwable error = result.getThrowable();
        if (error == null) {
            return "no reason given";
        }
        String message = String.valueOf(error.getMessage());
        return error.getClass().getSimpleName() + ": " + message.lines().findFirst().orElse("");
    }
}
