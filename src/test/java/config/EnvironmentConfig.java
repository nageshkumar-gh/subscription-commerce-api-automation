package config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

//Loads config/<env>.properties from the classpath. Pick the environment with -Denv=qa (default: local)
//Secrets (passwords) live in config/<env>-secrets.properties, which is gitignored and optional
public final class EnvironmentConfig {

    private static final String ENV = System.getProperty("env", "local");
    private static final Properties PROPERTIES = load();

    private EnvironmentConfig() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (!loadInto(properties, "config/" + ENV + ".properties")) {
            throw new IllegalStateException("Config file not found on classpath: config/" + ENV + ".properties");
        }
        //Values in the secrets file override the shared file
        loadInto(properties, "config/" + ENV + "-secrets.properties");
        return properties;
    }

    private static boolean loadInto(Properties properties, String path) {
        try (InputStream in = EnvironmentConfig.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return false;
            }
            properties.load(in);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    //A -Dkey=value system property wins over both files
    public static String get(String key) {
        String value = System.getProperty(key, PROPERTIES.getProperty(key));
        if (value == null) {
            throw new IllegalStateException("Missing config key '" + key + "' for env '" + ENV + "'. Add it to config/"
                    + ENV + ".properties, config/" + ENV + "-secrets.properties (for secrets) or pass -D" + key + "=...");
        }
        return value.trim();
    }

    public static String baseUrl() {
        return get("base.url");
    }

    public static String userEmail() {
        return get("user.email");
    }

    public static String userPassword() {
        return get("user.password");
    }
}
