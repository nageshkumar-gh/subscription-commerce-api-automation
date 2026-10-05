package tests.product;

import clients.ProductClient;
import io.restassured.response.Response;
import models.EsimPlanResponse;
import models.ProductResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.CatalogueData;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static core.Groups.*;
import static utilities.ApiAssertions.assertProductError;

public class ProductCatalogueTest {

    @Test(groups = {"PRD-PUB-01", SMOKE, PRODUCT, P1, REGRESSION}, description = "PRD-PUB-01: products are listed without a token, as JSON, and only active ones")
    public void listActiveProductsWithoutLogin() {
        Response response = ProductClient.getProducts();

        Assert.assertEquals(response.statusCode(), 200);
        //Unknown paths on this server also return 200 with an HTML page, so check the content type too
        Assert.assertTrue(response.contentType().contains("application/json"), "Expected JSON but got " + response.contentType());
        List<ProductResponse> products = List.of(response.as(ProductResponse[].class));
        Assert.assertFalse(products.isEmpty(), "Product list should not be empty");
        for (ProductResponse product : products) {
            Assert.assertTrue(product.active(), "Inactive product in public list: " + product.sku());
        }
    }

    @Test(groups = {"PRD-PUB-01", PRODUCT, P1, REGRESSION}, description = "PRD-PUB-01: every product has the required fields")
    public void everyProductHasRequiredFields() {
        for (ProductResponse product : CatalogueData.products()) {
            String label = "Product " + product.id();
            Assert.assertFalse(isBlank(product.id()), "Product without id: " + product);
            Assert.assertFalse(isBlank(product.sku()), label + " has no sku");
            Assert.assertFalse(isBlank(product.name()), label + " has no name");
            Assert.assertNotNull(product.price(), label + " has no price");
            Assert.assertTrue(product.price().compareTo(BigDecimal.ZERO) > 0, label + " price should be positive");
            Assert.assertTrue(product.features() != null && !product.features().isEmpty(), label + " has no features");
        }
    }

    @Test(groups = {"PRD-PUB-01", PRODUCT, P1, REGRESSION}, description = "PRD-PUB-01: SKUs are unique")
    public void productSkusAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ProductResponse product : CatalogueData.products()) {
            Assert.assertTrue(seen.add(product.sku()), "Duplicate sku: " + product.sku());
        }
    }

    @Test(groups = {"PRD-PUB-02", PRODUCT, P1, REGRESSION}, description = "PRD-PUB-02: get an active product by id returns the same product as the list")
    public void getProductByIdReturnsSameProductAsList() {
        ProductResponse expected = CatalogueData.firstProduct();

        Response response = ProductClient.getProductById(expected.id());

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.as(ProductResponse.class), expected);
    }

    @Test(groups = {"PRD-PUB-04", PRODUCT, P2, REGRESSION}, description = "PRD-PUB-04: an unknown product id gives 404 with the service's error body")
    public void getProductByUnknownIdReturns404() {
        assertProductError(ProductClient.getProductById("000000000000000000000000"), 404, "");
    }

    @Test(groups = {"PRD-PUB-05", SMOKE, PRODUCT, P1, REGRESSION}, description = "PRD-PUB-05: plans are listed without a token, only active ones, with required fields")
    public void listActivePlans() {
        Response response = ProductClient.getEsimPlans();

        Assert.assertEquals(response.statusCode(), 200);
        List<EsimPlanResponse> plans = List.of(response.as(EsimPlanResponse[].class));
        Assert.assertFalse(plans.isEmpty(), "Plan list should not be empty");
        for (EsimPlanResponse plan : plans) {
            Assert.assertTrue(plan.active(), "Inactive plan in public list: " + plan.code());
            Assert.assertFalse(isBlank(plan.code()), "Plan without code: " + plan);
            Assert.assertFalse(isBlank(plan.name()), "Plan without name: " + plan);
            Assert.assertTrue(plan.monthlyPrice() != null && plan.monthlyPrice().compareTo(BigDecimal.ZERO) > 0,
                    "Plan price should be positive: " + plan);
        }
    }

    @Test(groups = {"PRD-PUB-06", "PRD-PUB-07", PRODUCT, P2, REGRESSION}, description = "PRD-PUB-06, PRD-PUB-07: seeded phones and plans are present with exact decimal prices")
    public void seedDataIsPresentWithExactPrices() {
        Map<String, BigDecimal> productPrices = CatalogueData.products().stream()
                .collect(Collectors.toMap(ProductResponse::sku, ProductResponse::price));
        Map<String, BigDecimal> planPrices = CatalogueData.plans().stream()
                .collect(Collectors.toMap(EsimPlanResponse::code, EsimPlanResponse::monthlyPrice, (a, b) -> a));

        assertPrice(productPrices, "IPHONE-18-PRO-512", "899.00");
        assertPrice(productPrices, "IPHONE-18-PRO-MAX-1TB", "1299.00");
        assertPrice(planPrices, "LIMITED-2GB-DAY", "14.99");
        assertPrice(planPrices, "UNLIMITED", "29.99");
    }

    private static void assertPrice(Map<String, BigDecimal> prices, String key, String expected) {
        Assert.assertTrue(prices.containsKey(key), key + " missing from catalogue " + prices.keySet());
        Assert.assertEquals(prices.get(key).compareTo(new BigDecimal(expected)), 0, key + " price " + prices.get(key));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
