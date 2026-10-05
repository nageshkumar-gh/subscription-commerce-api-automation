package core;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import config.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IConfigurationListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//Writes the HTML report target/reports/extent-report.html. Registered in META-INF/services/org.testng.ITestNGListener
//Each test is titled by its description (which starts with the scenario ID) and shows its class.method,
//tags (service, priority, smoke/regression), result and duration;
//failures also show the error and the HTTP calls that led to it
public class ExtentReportListener implements ISuiteListener, ITestListener, IConfigurationListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentReportListener.class);
    private static final Path REPORT = Path.of("target", "reports", "extent-report.html");
    //Scenario ID tags such as CUS-LOG-01, ORC-STO-15, E2E-06, SMK-01
    private static final Pattern SCENARIO_ID = Pattern.compile("[A-Z0-9]+(-[A-Z]+)?-\\d+");
    private static final String EXTENT_TEST = "extentTest";

    private static ExtentReports extent;

    @Override
    public synchronized void onStart(ISuite suite) {
        if (extent != null) {
            return;
        }
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT.toString());
        spark.config().setDocumentTitle("API Automation Report");
        spark.config().setReportName("Subscription Commerce API tests");
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", EnvironmentConfig.environment());
        extent.setSystemInfo("Base URL", EnvironmentConfig.baseUrl());
        extent.setSystemInfo("Groups run", System.getProperty("groups", "all"));
        extent.setSystemInfo("Java", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
    }

    @Override
    public synchronized void onFinish(ISuite suite) {
        if (extent != null) {
            extent.flush();
            LOG.info("Extent report: {}", REPORT.toAbsolutePath());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        testFor(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testFor(result).pass("Passed in " + duration(result) + " ms");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = testFor(result);
        test.fail(result.getThrowable());
        attachHttpLog(test, result);
    }

    //Also called without onTestStart, e.g. when a failed @BeforeClass skips the class's tests
    @Override
    public void onTestSkipped(ITestResult result) {
        Throwable reason = result.getThrowable();
        if (reason == null) {
            testFor(result).skip("Skipped");
        } else {
            testFor(result).skip(reason);
        }
    }

    //A failed setup gets its own entry, so the reason its tests were skipped is visible in the report
    @Override
    public void onConfigurationFailure(ITestResult result) {
        ExtentTest test = create("Setup failed: " + className(result) + "." + result.getMethod().getMethodName(), null);
        test.assignCategory("setup");
        test.fail(result.getThrowable());
        attachHttpLog(test, result);
    }

    //One report entry per test result, kept on the result so every callback updates the same entry.
    //Title = the test's description; the class and method go underneath
    private static ExtentTest testFor(ITestResult result) {
        ExtentTest test = (ExtentTest) result.getAttribute(EXTENT_TEST);
        if (test == null) {
            test = create(title(result), className(result) + "." + result.getMethod().getMethodName());
            test.assignCategory(categories(result));
            result.setAttribute(EXTENT_TEST, test);
        }
        return test;
    }

    private static synchronized ExtentTest create(String name, String description) {
        return description == null ? extent.createTest(name) : extent.createTest(name, description);
    }

    private static void attachHttpLog(ExtentTest test, ITestResult result) {
        String log = HttpLog.forFailedResult(result);
        if (!log.isBlank()) {
            test.info("HTTP calls before the failure:");
            test.info(MarkupHelper.createCodeBlock(log));
        }
    }

    //The @Test description, e.g. "CUS-LOG-01: valid credentials return a new token for the same customer",
    //plus data-provider values so each data-driven row is distinguishable.
    //Falls back to "<scenario IDs> Class.method" for a test without a description
    private static String title(ITestResult result) {
        String description = result.getMethod().getDescription();
        String title = description != null && !description.isBlank() ? description : fallbackTitle(result);
        String parameters = result.getParameters().length == 0 ? "" : Arrays.stream(result.getParameters())
                .map(String::valueOf)
                .collect(Collectors.joining(", ", " [", "]"));
        return title + parameters;
    }

    private static String fallbackTitle(ITestResult result) {
        String ids = Arrays.stream(result.getMethod().getGroups())
                .filter(group -> SCENARIO_ID.matcher(group).matches())
                .collect(Collectors.joining(" "));
        return (ids.isEmpty() ? "" : ids + " ") + className(result) + "." + result.getMethod().getMethodName();
    }

    //Service, priority and suite tags become report categories; scenario IDs are already in the title
    private static String[] categories(ITestResult result) {
        List<String> tags = Arrays.stream(result.getMethod().getGroups())
                .filter(group -> !SCENARIO_ID.matcher(group).matches())
                .toList();
        return tags.toArray(String[]::new);
    }

    private static String className(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName();
    }

    private static long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
