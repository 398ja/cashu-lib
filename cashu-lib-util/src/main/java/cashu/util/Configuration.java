package cashu.util;

import lombok.NonNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Configuration {

    private Properties properties;

    private Configuration(@NonNull InputStream file) {
        init(file);
    }

    public static Configuration load(@NonNull InputStream file) {
        return new Configuration(file);
    }

    private void init(@NonNull InputStream file) {
        properties = new Properties();
        try {
            properties.load(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration file: " + file, e);
        }
    }

    public String getValue(@NonNull String key) {
        return properties.getProperty(key);
    }

    public List<String> getValues(@NonNull String key) {
        return Arrays.stream(properties.getProperty(key).split(",")).toList();
    }

    public Map<String, String> getMatching(@NonNull String keyPrefix) {
        Map<String, String> matchingKeys = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(keyPrefix)) {
                matchingKeys.put(key, properties.getProperty(key));
            }
        }
        return matchingKeys;
    }

}
