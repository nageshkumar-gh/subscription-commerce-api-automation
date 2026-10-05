package clients;

import core.BaseClient;
import io.restassured.response.Response;

import static endpoints.Routes.PROFILE;

//Profile operations for the customer who owns the token
public class CustomerClient extends BaseClient {

    //Profile of the configured account in local.properties
    public static Response getCustomer() {
        return authorizedRequest()
                .when()
                .get(PROFILE);
    }

    public static Response getCustomer(String token) {
        return authorizedRequest(token)
                .when()
                .get(PROFILE);
    }

    public static Response updateCustomer(String token, Object customerBody) {
        return authorizedRequest(token)
                .body(customerBody)
                .when()
                .put(PROFILE);
    }

    public static Response deleteCustomer(String token) {
        return authorizedRequest(token)
                .when()
                .delete(PROFILE);
    }
}
