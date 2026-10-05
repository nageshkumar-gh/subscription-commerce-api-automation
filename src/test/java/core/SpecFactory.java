package core;

import config.EnvironmentConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import utilities.TokenManager;

//Builds the request setup shared by every call, so clients never repeat base URL, headers or logging
public final class SpecFactory {

    private static RequestSpecification baseSpec;

    private SpecFactory() {
    }

    //Base URL, JSON headers, timeouts and request/response logging (see HttpLog for the modes)
    public static synchronized RequestSpecification baseSpec() {
        if (baseSpec == null) {
            baseSpec = new RequestSpecBuilder()
                    .setBaseUri(EnvironmentConfig.baseUrl())
                    .setConfig(withTimeouts())
                    .setContentType(ContentType.JSON)
                    .setAccept(ContentType.JSON)
                    .addFilters(HttpLog.filters())
                    .build();
        }
        return baseSpec;
    }

    //Without timeouts a dead server makes every test hang forever instead of failing
    private static RestAssuredConfig withTimeouts() {
        return RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", Integer.parseInt(EnvironmentConfig.get("http.connect.timeout.ms")))
                .setParam("http.socket.timeout", Integer.parseInt(EnvironmentConfig.get("http.read.timeout.ms"))));
    }

    //Base spec plus the bearer token of the configured user
    public static RequestSpecification authSpec() {
        return authSpec(TokenManager.getToken());
    }

    //Base spec plus a specific bearer token, e.g. a customer created by the test
    public static RequestSpecification authSpec(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(baseSpec())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
