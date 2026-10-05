package clients;

import core.BaseClient;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;

//Any method on any path. For platform checks (routing, wrong methods, CORS) rather than business calls
public class GenericClient extends BaseClient {

    public static Response send(Method method, String path, String token) {
        return authorizedRequest(token)
                .when()
                .request(method, path);
    }

    public static Response sendRaw(Method method, String path, String token, String body, ContentType contentType) {
        return authorizedRequest(token)
                .contentType(contentType)
                .body(body)
                .when()
                .request(method, path);
    }

    //CORS preflight as a browser on another site would send it
    public static Response preflight(String path, String origin, String requestedMethod) {
        return request()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", requestedMethod)
                .when()
                .options(path);
    }
}
