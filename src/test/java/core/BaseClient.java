package core;

import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

//Parent of every endpoint client. Gives a ready-made request so clients only add the path and body
public abstract class BaseClient {

    protected static RequestSpecification request() {
        return given().spec(SpecFactory.baseSpec());
    }

    protected static RequestSpecification authorizedRequest() {
        return given().spec(SpecFactory.authSpec());
    }

    //A null token sends no Authorization header, which security tests use for the "no token" cases
    protected static RequestSpecification authorizedRequest(String token) {
        return token == null ? request() : given().spec(SpecFactory.authSpec(token));
    }
}
