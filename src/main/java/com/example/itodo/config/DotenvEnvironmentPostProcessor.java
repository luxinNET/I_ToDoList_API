package com.example.itodo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a local `.env` file (project root) into the Spring Environment.
 *
 * <p>This keeps real secrets out of committed config files (the repo is public):
 * credentials live in gitignored `.env`, while `application*.yml` only reference
 * them via `${WECHAT_MINI_PROGRAM_APP_ID:}` placeholders. The `.env` is optional —
 * if it is absent the post-processor is a no-op, so packaged/production runs are
 * unaffected (they rely on real OS environment variables).</p>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DOTENV_FILE = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path path = Path.of(DOTENV_FILE);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                props.put(key, value);
            }
        } catch (IOException e) {
            // .env is optional; ignore read failures.
            return;
        }

        if (props.isEmpty()) {
            return;
        }

        PropertySource<?> existing = environment.getPropertySources().get(PROPERTY_SOURCE_NAME);
        if (existing != null) {
            environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        }
        // addFirst => .env values win for local dev (convenient override of defaults).
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
