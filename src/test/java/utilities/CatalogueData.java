package utilities;

import clients.ProductClient;
import models.EsimPlanResponse;
import models.ProductResponse;

import java.util.List;

//Reads the public catalogue, for tests that need a real product or plan to order
public final class CatalogueData {

    private CatalogueData() {
    }

    public static List<ProductResponse> products() {
        return List.of(ProductClient.getProducts().as(ProductResponse[].class));
    }

    public static List<EsimPlanResponse> plans() {
        return List.of(ProductClient.getEsimPlans().as(EsimPlanResponse[].class));
    }

    public static ProductResponse firstProduct() {
        return products().get(0);
    }

    public static EsimPlanResponse firstPlan() {
        return plans().get(0);
    }
}
