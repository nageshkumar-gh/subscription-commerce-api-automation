package utilities;

import clients.AuthClient;
import clients.CustomerClient;
import io.restassured.response.Response;
import models.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//Registers fresh customers for tests and deletes them when the suite finishes (see core.CleanupListener)
public final class CustomerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerFactory.class);
    private static final List<String> TOKENS_TO_DELETE = new CopyOnWriteArrayList<>();

    private CustomerFactory() {
    }

    public static TestCustomer create() {
        RegisterRequest registerRequest = TestData.newRegisterRequest();
        Response response = AuthClient.register(registerRequest);
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Could not register test customer: " + response.statusCode() + " " + response.asString());
        }
        String token = response.jsonPath().getString("accessToken");
        trackForCleanup(token);
        LOG.debug("Registered test customer {} ({})", response.jsonPath().getString("customer.id"), registerRequest.email());
        return new TestCustomer(
                response.jsonPath().getString("customer.id"),
                registerRequest.name(),
                response.jsonPath().getString("customer.email"),
                registerRequest.phone(),
                registerRequest.password(),
                token);
    }

    //For customers a test registers itself (e.g. registration tests)
    public static void trackForCleanup(String token) {
        if (token != null) {
            TOKENS_TO_DELETE.add(token);
        }
    }

    //Customers already deleted by a test just return 404, which is fine
    public static void deleteAll() {
        LOG.info("Cleaning up {} test customers", TOKENS_TO_DELETE.size());
        for (String token : TOKENS_TO_DELETE) {
            try {
                int status = CustomerClient.deleteCustomer(token).statusCode();
                if (status != 204 && status != 404) {
                    LOG.warn("Could not delete a test customer: HTTP {}", status);
                }
            } catch (RuntimeException e) {
                //Keep cleaning up the rest even if the server is unreachable for one call
                LOG.warn("Could not delete a test customer: {}", e.getMessage());
            }
        }
        TOKENS_TO_DELETE.clear();
    }
}
