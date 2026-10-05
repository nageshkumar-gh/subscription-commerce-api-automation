package tests.platform;

import clients.GenericClient;
import config.EnvironmentConfig;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.TestCustomer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import static core.Groups.*;

//Edge and network checks: internal services must only be reachable through the proxies
public class DeploymentTest {

    private static final int CONNECT_TIMEOUT_MS = 3000;

    @DataProvider
    public Object[][] internalPaths() {
        return new Object[][]{
                {"/api/orders"},
                {"/api/payments"},
                {"/api/workflows/orders/000000000000000000000000"},
                {"/api/activations"},
                {"/api/fulfillments"},
                {"/api/subscriptions"},
                {"/api/tracking/orders"},
                {"/api/billing-runs"},
                {"/api/invoices"},
        };
    }

    @Test(groups = {"E2E-06", PLATFORM, P1, REGRESSION}, dataProvider = "internalPaths", description = "E2E-06: the web-ui proxy does not route operations APIs")
    public void webProxyDoesNotRouteOperationsApis(String path) {
        TestCustomer customer = CustomerFactory.create();

        Response response = GenericClient.send(Method.GET, path, customer.token());

        //Not routed means 404 or the SPA's HTML page; JSON with 2xx would mean the internal service answered
        boolean reachedService = response.statusCode() < 300 && String.valueOf(response.contentType()).contains("json");
        Assert.assertFalse(reachedService, path + " reached an internal service through the web proxy: " + response.asString());
    }

    @DataProvider
    public Object[][] servicePorts() {
        return new Object[][]{
                {8082, "order-service"},
                {8083, "payment-service"},
                {8084, "network-service"},
                {8085, "fulfillment-service"},
                {8086, "billing-service"},
                {8087, "orchestration-service"},
                {8089, "invoice-service"},
        };
    }

    @Test(groups = {"E2E-06", PLATFORM, P1, REGRESSION}, dataProvider = "servicePorts", description = "E2E-06: internal service ports are not reachable from outside")
    public void servicePortIsNotReachable(int port, String service) {
        assertNotReachable(port, service);
    }

    @Test(groups = {"TRK-SEC-01", "E2E-06", PLATFORM, TRACKING, P1, REGRESSION}, description = "TRK-SEC-01: tracking-service port 8088, which serves every customer's events without a token, is not reachable")
    public void trackingPortIsNotReachable() {
        assertNotReachable(8088, "tracking-service");
    }

    @DataProvider
    public Object[][] infrastructurePorts() {
        return new Object[][]{
                {9092, "Kafka"},
                {7233, "Temporal"},
                {27017, "MongoDB"},
                {5432, "PostgreSQL"},
        };
    }

    //Not in the design doc (framework ID DEP-01): infrastructure should be private too
    @Test(groups = {"DEP-01", PLATFORM, P1, REGRESSION}, dataProvider = "infrastructurePorts", description = "DEP-01: Kafka, Temporal and database ports are not reachable from outside")
    public void infrastructurePortIsNotReachable(int port, String component) {
        assertNotReachable(port, component);
    }

    private static void assertNotReachable(int port, String component) {
        String host = URI.create(EnvironmentConfig.baseUrl()).getHost();

        Assert.assertFalse(isReachable(host, port), component + " is reachable from outside on " + host + ":" + port);
    }

    private static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
