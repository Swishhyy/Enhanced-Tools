package me.swishhyy.enhancedtools;

import me.swishhyy.enhancedtools.commands.EnhancedToolsCommand;
import me.swishhyy.enhancedtools.commands.UpgradeCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class EnhancedTools extends JavaPlugin {

    @Override
    public void onEnable() {
        // This copies config.yml from your jar to plugins/EnhancedTools/ if it doesn't exist
        saveDefaultConfig();

        // Register the standalone "/upgrade" command, passing the plugin instance
        Objects.requireNonNull(getCommand("upgrade")).setExecutor(new UpgradeCommand(this));

        // Register the main "/enhancedtools" command, passing 'this' as the plugin reference
        Objects.requireNonNull(getCommand("enhancedtools")).setExecutor(new EnhancedToolsCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
