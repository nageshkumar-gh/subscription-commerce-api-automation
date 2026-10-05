package utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;

//Error bodies differ per service (see the design doc), so each service has its own check
public final class ApiAssertions {

    private ApiAssertions() {
    }

    //customer-service: timestamp, status, error, message, path
    public static void assertCustomerError(Response response, int status, String expectedMessagePart) {
        assertErrorBody(response, status, expectedMessagePart, List.of("timestamp", "error", "path"));
    }

    //product, network, fulfillment, billing: timestamp, status, error, message
    public static void assertProductError(Response response, int status, String expectedMessagePart) {
        assertErrorBody(response, status, expectedMessagePart, List.of("timestamp", "error"));
    }

    //orchestration and invoice: status, message
    public static void assertStorefrontError(Response response, int status, String expectedMessagePart) {
        assertErrorBody(response, status, expectedMessagePart, List.of());
    }

    public static void assertStatusIn(Response response, Integer... allowed) {
        Assert.assertTrue(Arrays.asList(allowed).contains(response.statusCode()),
                "Expected status in " + Arrays.toString(allowed) + " but got " + response.statusCode() + ": " + response.asString());
    }

    private static void assertErrorBody(Response response, int status, String expectedMessagePart, List<String> extraFields) {
        Assert.assertEquals(response.statusCode(), status, "Unexpected status. Body: " + response.asString());
        JsonPath body = response.jsonPath();
        Assert.assertEquals(body.getInt("status"), status, "status field in error body");
        String message = body.getString("message");
        Assert.assertTrue(message != null && message.contains(expectedMessagePart),
                "Expected message containing '" + expectedMessagePart + "' but got '" + message + "'");
        for (String field : extraFields) {
            Assert.assertNotNull(body.get(field), "Missing '" + field + "' in error body: " + response.asString());
        }
    }
}
