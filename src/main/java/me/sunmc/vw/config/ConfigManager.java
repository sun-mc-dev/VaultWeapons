package me.sunmc.vw.config;

import me.sunmc.vw.VaultWeaponsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all configuration files for VaultWeapons
 */
public class ConfigManager {

    private final VaultWeaponsPlugin plugin;
    private final ConcurrentHashMap<String, FileConfiguration> configs;
    private final ConcurrentHashMap<String, File> configFiles;

    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration weaponsConfig;

    public ConfigManager(@NotNull VaultWeaponsPlugin plugin) {
        this.plugin = plugin;
        this.configs = new ConcurrentHashMap<>();
        this.configFiles = new ConcurrentHashMap<>();
    }

    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        saveDefaultConfig("config.yml");
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("weapons.yml");

        mainConfig = loadConfig("config.yml");
        messagesConfig = loadConfig("messages.yml");
        weaponsConfig = loadConfig("weapons.yml");
    }

    /**
     * Save default config from resources if it doesn't exist
     *
     * @param fileName The config file name
     */
    private void saveDefaultConfig(@NotNull String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        configFiles.put(fileName, file);
    }

    /**
     * Load a configuration file
     *
     * @param fileName The config file name
     * @return The FileConfiguration
     */
    @NotNull
    public FileConfiguration loadConfig(@NotNull String fileName) {
        File file = configFiles.get(fileName);
        if (file == null) {
            file = new File(plugin.getDataFolder(), fileName);
            configFiles.put(fileName, file);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from jar
        Reader defaultConfigStream = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(fileName)),
                StandardCharsets.UTF_8
        );
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
            config.setDefaults(defaultConfig);
        }

        configs.put(fileName, config);
        return config;
    }

    /**
     * Save a configuration file
     *
     * @param fileName The config file name
     */
    public void saveConfig(@NotNull String fileName) {
        FileConfiguration config = configs.get(fileName);
        File file = configFiles.get(fileName);

        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save config: " + fileName);
                e.printStackTrace();
            }
        }
    }

    /**
     * Reload all configuration files
     */
    public void reloadConfigs() {
        loadConfigs();
    }

    /**
     * Get the main configuration
     *
     * @return The main FileConfiguration
     */
    @NotNull
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    /**
     * Get the messages configuration
     *
     * @return The messages FileConfiguration
     */
    @NotNull
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Get the weapons configuration
     *
     * @return The weapons FileConfiguration
     */
    @NotNull
    public FileConfiguration getWeaponsConfig() {
        return weaponsConfig;
    }

    /**
     * Get a message from the messages config with color codes translated
     *
     * @param path The path to the message
     * @return The translated message
     */
    @NotNull
    public String getMessage(@NotNull String path) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return translateColors(message);
    }

    /**
     * Get a message with placeholders replaced
     *
     * @param path         The path to the message
     * @param placeholders The placeholders to replace (key, value pairs)
     * @return The translated message with placeholders replaced
     */
    @NotNull
    public String getMessage(@NotNull String path, @NotNull String @NotNull ... placeholders) {
        String message = getMessage(path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
        }
        return message;
    }

    /**
     * Translate color codes in a string
     *
     * @param text The text to translate
     * @return The translated text
     */
    @NotNull
    public static String translateColors(@NotNull String text) {
        return text.replace("&", "§");
    }

    /**
     * Get a config value with a default
     *
     * @param path         The path to the value
     * @param defaultValue The default value
     * @param <T>          The type of the value
     * @return The value or default
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(@NotNull String path, @Nullable T defaultValue) {
        Object value = mainConfig.get(path, defaultValue);
        return value != null ? (T) value : defaultValue;
    }
}