package me.swishhyy.enhancedtools.commands;

import me.swishhyy.enhancedtools.EnhancedTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnhancedToolsCommand implements CommandExecutor, TabExecutor {

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
            sender.sendMessage(plugin.getMessage("commands.enhancedtools.usage", "Use /enhancedtools reload to reload the plugin."));
            return true;
        }

        // Check subcommand
        if (args[0].equalsIgnoreCase("reload")) {
            // Perform reload logic, if they have permission
            if (!sender.hasPermission("enhancedtools.command.admin")) {
                sender.sendMessage(plugin.getMessage("commands.enhancedtools.no_permission", "You do not have permission to use this command!"));
                return true;
            }

            // Reload configuration
            plugin.reloadConfig();
            sender.sendMessage(plugin.getMessage("commands.enhancedtools.reload_success", "EnhancedTools configuration reloaded successfully!"));
            return true;
        }

        // If we reach here, user typed an unknown subcommand
        sender.sendMessage(plugin.getMessage("commands.enhancedtools.unknown_subcommand", "Unknown subcommand! Usage: /enhancedtools [reload]"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        // Provide suggestions for the first argument
        if (args.length == 1) {
            return Stream.of("reload")
                    .filter(subcommand -> subcommand.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Return an empty list for other cases
        return Collections.emptyList();
    }
}
