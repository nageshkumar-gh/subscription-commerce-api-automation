package tests.platform;

import clients.CustomerClient;
import clients.ProductClient;
import config.EnvironmentConfig;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static core.Groups.*;

//Quick "is the environment usable" checks (framework IDs SMK-xx; not in the design doc)
public class SmokeTest {

    @Test(groups = {"SMK-01", SMOKE, PLATFORM, P1, REGRESSION}, description = "SMK-01: the configured test account logs in and reads its profile")
    public void configuredAccountCanReadProfile() {
        Response response = CustomerClient.getCustomer();

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getString("email").equalsIgnoreCase(EnvironmentConfig.userEmail()));
    }

    @Test(groups = {"SMK-02", SMOKE, PLATFORM, P1, REGRESSION}, description = "SMK-02: products and eSIM plans are served as JSON")
    public void catalogueIsServed() {
        for (Response response : new Response[]{ProductClient.getProducts(), ProductClient.getEsimPlans()}) {
            Assert.assertEquals(response.statusCode(), 200);
            Assert.assertTrue(response.contentType().contains("application/json"), "Content type: " + response.contentType());
        }
    }
}
