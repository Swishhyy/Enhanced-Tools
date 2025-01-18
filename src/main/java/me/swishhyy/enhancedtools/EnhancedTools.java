package me.swishhyy.enhancedtools;

import me.swishhyy.enhancedtools.commands.EnhancedToolsCommand;
import me.swishhyy.enhancedtools.commands.UpgradeCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class EnhancedTools extends JavaPlugin {

    private Economy economy;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        // Save default configuration
        saveDefaultConfig();
        loadMessages();

        getLogger().info(getMessage("plugin.enabled", "{plugin_name} has been enabled!"));

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

        // Register the "/upgrade" command
        Objects.requireNonNull(getCommand("upgrade")).setExecutor(new UpgradeCommand(this));

        // Register the "/enhancedtools" command
        Objects.requireNonNull(getCommand("enhancedtools")).setExecutor(new EnhancedToolsCommand(this));
    }

    @Override
    public void onDisable() {
        // Send a message to the console
        getLogger().info(getMessage("plugin.disabled", "{plugin_name} has been disabled!"));
        getServer().getConsoleSender().sendMessage(getMessage("plugin.unloaded", "§4§l{plugin_name} has unloaded successfully."));
    }

    private void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, String defaultMessage) {
        String message = messagesConfig.getString(key, defaultMessage);
        return message.replace("{plugin_name}", getFormattedPluginName());
    }

    public static String getFormattedPluginName() {
        return "§5§lEnhanced§e§lTools";
    }

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

    public Economy getEconomy() {
        return economy;
    }

    public void reloadMessages() {
        try {
            messagesConfig.load(messagesFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            getLogger().severe("Failed to reload messages.yml!");
        }
    }
}
