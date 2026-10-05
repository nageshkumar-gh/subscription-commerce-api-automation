package core;

import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

//Prints the buffered HTTP log of a failed test or failed setup (http.log=failures). Passing tests stay quiet.
//Registered in META-INF/services/org.testng.ITestNGListener
public class HttpLogListener implements ITestListener, IConfigurationListener {

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
        print("test " + name(result));
    }

    //A failed @BeforeClass (e.g. could not create a customer) skips its tests, so show what it sent
    @Override
    public void onConfigurationFailure(ITestResult result) {
        print("setup " + name(result));
    }

    @Override
    public void onConfigurationSuccess(ITestResult result) {
        HttpLog.clear();
    }

    private static void print(String what) {
        if (!HttpLog.isBuffered()) {
            return;
        }
        String log = HttpLog.drain();
        System.out.println("\n========== HTTP log for failed " + what + " ==========\n"
                + (log.isBlank() ? "(no HTTP calls)\n" : log)
                + "========== end of HTTP log ==========\n");
    }

    private static String name(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }
}
