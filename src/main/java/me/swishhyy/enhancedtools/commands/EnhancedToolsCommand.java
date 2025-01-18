package me.swishhyy.enhancedtools.commands;

import me.swishhyy.enhancedtools.EnhancedTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EnhancedToolsCommand implements CommandExecutor {

    private final EnhancedTools plugin;

    public EnhancedToolsCommand(EnhancedTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            // No subcommands provided, show usage or help
            sender.sendMessage("Use /enhancedtools reload to reload the plugin.");
            return true;
        }

        // Check subcommand
        if (args[0].equalsIgnoreCase("reload")) {
            // Perform reload logic, if they have permission
            if (!sender.hasPermission("enhancedtools.command.admin")) {
                sender.sendMessage("You do not have permission to use this command!");
                return true;
            }

            // Reload configuration
            plugin.reloadConfig();
            sender.sendMessage("EnhancedTools configuration reloaded successfully!");
            return true;
        }

        // If we reach here, user typed an unknown subcommand
        sender.sendMessage("Unknown subcommand! Usage: /enhancedtools [reload]");
        return true;
    }
}
