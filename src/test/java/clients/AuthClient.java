package clients;

import core.BaseClient;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static endpoints.Routes.LOGIN;
import static endpoints.Routes.REGISTER;
import static endpoints.Routes.VERIFY;

//Bodies are Object so tests can send a model (RegisterRequest, LoginRequest) or a Map with extra fields
public class AuthClient extends BaseClient {

    public static Response register(Object registerBody) {
        return request()
                .body(registerBody)
                .when()
                .post(REGISTER);
    }

    //For malformed JSON and wrong Content-Type checks
    public static Response registerWithRawBody(String body, ContentType contentType) {
        return request()
                .contentType(contentType)
                .body(body)
                .when()
                .post(REGISTER);
    }

    public static Response login(Object loginBody) {
        return request()
                .body(loginBody)
                .when()
                .post(LOGIN);
    }

    //Edge token check used by the admin console. A null scope sends no scope parameter
    public static Response verify(String token, String scope) {
        RequestSpecification request = authorizedRequest(token);
        if (scope != null) {
            request.queryParam("scope", scope);
        }
        return request
                .when()
                .get(VERIFY);
    }
}
