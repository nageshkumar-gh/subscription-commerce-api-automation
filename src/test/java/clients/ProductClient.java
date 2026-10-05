package clients;

import core.BaseClient;
import io.restassured.response.Response;

import static endpoints.Routes.ADMIN_PRODUCTS;
import static endpoints.Routes.ESIM_PLANS;
import static endpoints.Routes.PRODUCTS;
import static endpoints.Routes.PRODUCT_BY_ID;

//Product catalogue operations. Reading products is public, so no token is sent
public class ProductClient extends BaseClient {

    public static Response getProducts() {
        return request()
                .when()
                .get(PRODUCTS);
    }

    public static Response getProductById(String id) {
        return request()
                .pathParam("id", id)
                .when()
                .get(PRODUCT_BY_ID);
    }

    public static Response getEsimPlans() {
        return request()
                .when()
                .get(ESIM_PLANS);
    }

    //Admin list (needs scope catalog:write). Used by security tests with no token or the wrong token
    public static Response getAdminProducts(String token) {
        return authorizedRequest(token)
                .when()
                .get(ADMIN_PRODUCTS);
    }
}
