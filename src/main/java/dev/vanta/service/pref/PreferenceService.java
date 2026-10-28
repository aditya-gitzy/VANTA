package dev.vanta.service.pref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lightweight local preference persistence storing user state (e.g. last open module)
 * in user's home directory (~/.vanta/preferences.properties).
 */
public class PreferenceService {

    private static final String DIR_NAME = ".vanta";
    private static final String FILE_NAME = "preferences.properties";
    private static final String KEY_LAST_MODULE = "last.module.id";

    private final Path configPath;
    private final Properties properties = new Properties();

    public PreferenceService() {
        this(Path.of(System.getProperty("user.home"), DIR_NAME, FILE_NAME));
    }

    public PreferenceService(Path configPath) {
        this.configPath = configPath;
        load();
    }

    private synchronized void load() {
        if (Files.exists(configPath)) {
            try (FileInputStream in = new FileInputStream(configPath.toFile())) {
                properties.load(in);
            } catch (IOException ignored) {
                // Graceful fallback to default values
            }
        }
    }

    public synchronized void save() {
        try {
            if (configPath.getParent() != null && !Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            try (FileOutputStream out = new FileOutputStream(configPath.toFile())) {
                properties.store(out, "VANTA User Preferences");
            }
        } catch (IOException ignored) {
            // Non-fatal if writing preference fails
        }
    }

    public synchronized String getLastModuleId(String defaultValue) {
        return properties.getProperty(KEY_LAST_MODULE, defaultValue);
    }

    public synchronized void setLastModuleId(String moduleId) {
        if (moduleId != null) {
            properties.setProperty(KEY_LAST_MODULE, moduleId);
            save();
        }
    }
}
