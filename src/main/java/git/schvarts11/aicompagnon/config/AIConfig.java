package git.schvarts11.aicompagnon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import git.schvarts11.aicompagnon.AicompagnonMod;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads and exposes plugin configuration values with safe defaults.
 */
public final class AIConfig {

    private static AIConfig INSTANCE;

    private final FileConfiguration config;

    private AIConfig(FileConfiguration config) {
        this.config = config;
    }

    public static synchronized void init(JavaPlugin plugin) {
        if (INSTANCE != null) {
            throw new IllegalStateException("AIConfig already initialized");
        }
        plugin.saveDefaultConfig();
        INSTANCE = new AIConfig(plugin.getConfig());
    }

    public static AIConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AIConfig not initialized");
        }
        return INSTANCE;
    }

    public static synchronized boolean reload() {
        if (INSTANCE == null) {
            return false;
        }
        JavaPlugin plugin = AicompagnonMod.plugin;
        plugin.reloadConfig();
        INSTANCE = new AIConfig(plugin.getConfig());
        return true;
    }

    public String apiEndpoint() {
        return config.getString("api_endpoint", "http://localhost:11434").trim();
    }

    public String apiKey() {
        return config.getString("api_key", "").trim();
    }

    public String model() {
        return config.getString("model", "llama3").trim();
    }

    public int tickIntervalTicks() {
        return Math.max(1, config.getInt("tick_interval_ticks", 60));
    }

    public int maxCompanionsPerPlayer() {
        return Math.max(1, config.getInt("max_companions_per_player", 5));
    }

    public String companionPrefix() {
        return config.getString("companion_prefix", "<[AI]> ");
    }

    public boolean removeOnOwnerQuit() {
        return config.getBoolean("remove_on_owner_quit", false);
    }

    public List<String> allowedActions() {
        List<String> raw = config.getStringList("allowed_actions");
        if (raw == null || raw.isEmpty()) {
            return List.of("follow", "move_to", "look_at", "say", "wait", "wander");
        }
        return raw.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public int apiTimeoutSeconds() {
        return Math.max(1, config.getInt("api_timeout_seconds", 15));
    }

    public int contextRadiusBlocks() {
        return Math.max(1, config.getInt("context_radius_blocks", 16));
    }

    public int maxHistoryEntries() {
        return Math.max(1, config.getInt("max_history_entries", 20));
    }

    public void validate() {
        String endpoint = apiEndpoint();
        if (endpoint.isEmpty()) {
            AicompagnonMod.plugin.getLogger().warning("api_endpoint is empty; AI calls will fail until configured.");
        }
    }
}
