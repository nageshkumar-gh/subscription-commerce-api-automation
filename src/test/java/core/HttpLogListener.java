package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

//Logs the buffered HTTP traffic of a failed test or failed setup (http.log=failures). Passing tests stay quiet.
//Registered in META-INF/services/org.testng.ITestNGListener
public class HttpLogListener implements ITestListener, IConfigurationListener {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLogListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        HttpLog.clear();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        HttpLog.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logHttpTraffic("test " + name(result));
    }

    //A failed @BeforeClass (e.g. could not create a customer) skips its tests, so show what it sent
    @Override
    public void onConfigurationFailure(ITestResult result) {
        logHttpTraffic("setup " + name(result));
    }

    @Override
    public void onConfigurationSuccess(ITestResult result) {
        HttpLog.clear();
    }

    private static void logHttpTraffic(String what) {
        if (!HttpLog.isBuffered()) {
            return;
        }
        String log = HttpLog.drain();
        LOG.error("HTTP log for failed {}:\n{}", what, log.isBlank() ? "(no HTTP calls)" : log.stripTrailing());
    }

    private static String name(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }
}
