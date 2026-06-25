package com.entain.sportevents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the list of valid sport types from {@code application.yml}.
 * <p>
 * Example configuration:
 * <pre>
 * sport:
 *   types:
 *     - FOOTBALL
 *     - HOCKEY
 * </pre>
 * Adding a new sport type only requires editing {@code application.yml} — no code changes needed.
 */
@Component
@ConfigurationProperties(prefix = "sport")
public class SportTypesConfig {

    private List<String> types = new ArrayList<>();

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }
}
