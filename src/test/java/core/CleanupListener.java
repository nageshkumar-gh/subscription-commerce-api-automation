package core;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import utilities.CustomerFactory;

//Runs after every suite, including single-class runs. Registered in META-INF/services/org.testng.ITestNGListener
public class CleanupListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        CustomerFactory.deleteAll();
    }
}
