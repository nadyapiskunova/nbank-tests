package api.configs;

import api.contract.BackendVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();

    private Config() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if(input == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("File to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return INSTANCE.properties.getProperty(key);
    }

    public static BackendVersion getBackendVersion() {
        return BackendVersion.valueOf(getProperty("backend.version"));
    }
}
