package tests.product;

import clients.GenericClient;
import clients.ProductClient;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utilities.CatalogueData;
import utilities.CustomerFactory;
import utilities.TestCustomer;

import static core.Groups.*;
import static endpoints.Routes.PRODUCTS;
import static utilities.ApiAssertions.assertStatusIn;

public class ProductSecurityTest {

    private TestCustomer customer;

    @BeforeClass(alwaysRun = true)
    public void createCustomer() {
        customer = CustomerFactory.create();
    }

    @Test(groups = {"PRD-SEC-01", PRODUCT, P1, REGRESSION}, description = "PRD-SEC-01: admin endpoint without a token gives 401")
    public void adminWithoutTokenGives401() {
        assertStatusIn(ProductClient.getAdminProducts(null), 401);
    }

    @Test(groups = {"PRD-SEC-02", PRODUCT, P1, REGRESSION}, description = "PRD-SEC-02: admin endpoint with an ordinary customer token gives 403")
    public void adminWithCustomerTokenGives403() {
        assertStatusIn(ProductClient.getAdminProducts(customer.token()), 403);
    }

    @Test(groups = {"PRD-SEC-06", PRODUCT, P2, REGRESSION}, description = "PRD-SEC-06: writing to public catalogue paths is denied")
    public void writesToPublicPathsAreDenied() {
        String productId = CatalogueData.firstProduct().id();

        Response create = GenericClient.sendRaw(Method.POST, PRODUCTS, customer.token(), "{\"sku\":\"HACK\"}", ContentType.JSON);
        Response delete = GenericClient.send(Method.DELETE, PRODUCTS + "/" + productId, customer.token());

        assertStatusIn(create, 401, 403, 405);
        assertStatusIn(delete, 401, 403, 405);
    }
}
