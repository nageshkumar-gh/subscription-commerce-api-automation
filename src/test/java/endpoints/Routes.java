package endpoints;

//API paths only. The base URL comes from EnvironmentConfig
public final class Routes {

    private Routes() {
    }

    //customer-service
    public static final String LOGIN = "/api/auth/login";
    public static final String REGISTER = "/api/auth/register";
    public static final String VERIFY = "/api/auth/verify";
    public static final String CUSTOMERS = "/api/customers";
    public static final String PROFILE = "/api/customers/me";

    //product-service
    public static final String PRODUCTS = "/api/products";
    public static final String PRODUCT_BY_ID = "/api/products/{id}";
    public static final String ESIM_PLANS = "/api/esim-plans";
    public static final String ADMIN_PRODUCTS = "/api/admin/products";

    //Storefront (orchestration-service), always for the signed-in customer
    public static final String MY_ORDERS = "/api/me/orders";
    public static final String MY_ORDER = "/api/me/orders/{orderId}";
    public static final String MY_ORDER_CHECKOUT = "/api/me/orders/{orderId}/checkout";
    public static final String MY_ORDER_CANCEL = "/api/me/orders/{orderId}/cancel";
    public static final String MY_ORDER_EVENTS = "/api/me/orders/{orderId}/events";
    public static final String MY_SUBSCRIPTIONS = "/api/me/subscriptions";
}
