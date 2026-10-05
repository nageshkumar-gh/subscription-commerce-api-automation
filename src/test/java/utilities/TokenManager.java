package utilities;

import clients.AuthClient;
import config.EnvironmentConfig;
import io.restassured.response.Response;
import models.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Logs in once with the configured user and caches the access token for the whole run
public final class TokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);
    private static String token;

    private TokenManager() {
    }

    public static synchronized String getToken() {
        if (token == null) {
            LoginRequest loginRequest = new LoginRequest(EnvironmentConfig.userEmail(), EnvironmentConfig.userPassword());
            Response response = AuthClient.login(loginRequest);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Login failed with status " + response.statusCode() + ": " + response.asString());
            }
            token = response.jsonPath().getString("accessToken");
            LOG.info("Logged in configured account {}", EnvironmentConfig.userEmail());
        }
        return token;
    }
}
