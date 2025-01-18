package me.swishhyy.enhancedtools;

import me.swishhyy.enhancedtools.commands.EnhancedToolsCommand;
import me.swishhyy.enhancedtools.commands.UpgradeCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class EnhancedTools extends JavaPlugin {

    private Economy economy;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        // Save default configurations
        saveDefaultConfig();
        saveDefaultMessages(); // Save messages.yml if not present
        loadMessages();

        getLogger().info(getMessage("plugin.enabled", "EnhancedTools plugin has been enabled!"));

        // Check for Vault API and set "use-vault" in the config accordingly
        if (setupEconomy()) {
            getConfig().set("use-vault", true);
            getLogger().info(getMessage("vault.detected", "Vault API detected. Enabling currency-based upgrades."));
        } else {
            getConfig().set("use-vault", false);
            getLogger().info(getMessage("vault.not_detected", "Vault API not detected. Disabling currency-based upgrades."));
        }

        // Save the updated configuration
        saveConfig();

        // Register commands
        Objects.requireNonNull(getCommand("upgrade")).setExecutor(new UpgradeCommand(this));
        Objects.requireNonNull(getCommand("enhancedtools")).setExecutor(new EnhancedToolsCommand(this));
    }

    @Override
    public void onDisable() {
        // Send a bold red message to the console
        getServer().getConsoleSender().sendMessage(getMessage("plugin.unloaded", "§4§lEnhancedTools has unloaded successfully."));
        getLogger().info(getMessage("plugin.disabled", "EnhancedTools plugin has been disabled!"));
    }

    public static String getFormattedPluginName() {
        return "§5§lEnhanced§e§lTools";
    }

    /**
     * Saves the default messages.yml file if it doesn't exist.
     */
    private void saveDefaultMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }

    /**
     * Loads messages.yml into the configuration.
     */
    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Retrieves a message from messages.yml, with a fallback default value.
     *
     * @param key          The key of the message in messages.yml.
     * @param defaultValue The default message if the key is not found.
     * @return The formatted message.
     */
    public String getMessage(String key, String defaultValue) {
        if (messagesConfig == null) {
            return defaultValue.replace("{plugin_name}", getFormattedPluginName());
        }
        return messagesConfig.getString(key, defaultValue)
                .replace("{plugin_name}", getFormattedPluginName());
    }

    /**
     * Sets up the economy (Vault integration) if available.
     *
     * @return true if Vault and an Economy provider are detected; false otherwise.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    /**
     * Get the Economy instance (if needed elsewhere).
     *
     * @return Economy instance if available; null otherwise.
     */
    public Economy getEconomy() {
        return economy;
    }
}
