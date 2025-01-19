package me.swishhyy.enhancedtools.commands;

import me.swishhyy.enhancedtools.EnhancedTools;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class UpgradeCommand implements CommandExecutor, TabExecutor {

    private final EnhancedTools plugin;

    public UpgradeCommand(EnhancedTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("commands.enhancedtools.only-players", "{plugin_name} §7- Only players can use this command!")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName()));
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.holding-air", "{plugin_name} §7- You are not holding an item to upgrade!")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName()));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.invalid-usage", "{plugin_name} §7- Usage: §e/upgrade <enchant> <level>")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName()));
            return true;
        }

        String enchantName = args[0].toLowerCase();
        String levelString = args[1];

        int level;
        try {
            level = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.invalid-level", "{plugin_name} §7- Invalid level! Must be a number.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName()));
            return true;
        }

        Enchantment enchantment = matchCustomEnchant(enchantName);
        if (enchantment == null) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.unknown-enchantment", "{plugin_name} §7- Unknown enchantment: §c{enchant}")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{enchant}", enchantName));
            return true;
        }

        if (!enchantment.canEnchantItem(itemInHand)) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.cannot-apply", "{plugin_name} §7- §c{enchant} §7cannot be applied to §e{item}§7!")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{enchant}", enchantName)
                    .replace("{item}", itemInHand.getType().toString()));
            return true;
        }

        ConfigurationSection enchantsSection = plugin.getConfig().getConfigurationSection("enchants");
        if (enchantsSection == null) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.missing-enchants-section", "{plugin_name} §7- Configuration error: Missing 'enchants' section.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName()));
            return true;
        }

        if (!enchantsSection.contains(enchantName)) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.missing-enchant-config", "{plugin_name} §7- No configuration found for enchantment §c{enchant}§7.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{enchant}", enchantName));
            return true;
        }

        ConfigurationSection enchantConfig = enchantsSection.getConfigurationSection(enchantName);
        if (enchantConfig == null || !enchantConfig.getBoolean("enabled", true)) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.not-enabled", "{plugin_name} §7- The enchantment §c{enchant} §7is not enabled or cannot be upgraded!")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{enchant}", enchantName));
            return true;
        }

        if (!enchantConfig.contains("levels")) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.missing-upgrade-levels", "{plugin_name} §7- The enchantment §c{enchant} §7has no upgrade levels defined.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{enchant}", enchantName));
            return true;
        }

        ConfigurationSection levelConfig = enchantConfig.getConfigurationSection("levels." + level);
        if (levelConfig == null) {
            player.sendMessage(plugin.getMessage("commands.enhancedtools.invalid-upgrade-level", "{plugin_name} §7- Invalid level §c{level} §7for enchantment §c{enchant}§7.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{level}", String.valueOf(level))
                    .replace("{enchant}", enchantName));
            return true;
        }

        int xpCost = levelConfig.getInt("xp-cost");
        double currencyCost = levelConfig.getDouble("currency-cost");

        boolean useVault = plugin.getConfig().getBoolean("use-vault", false);
        if (useVault) {
            Economy economy = plugin.getEconomy();
            if (economy.getBalance(player) < currencyCost) {
                player.sendMessage(plugin.getMessage("commands.enhancedtools.not-enough-currency", "{plugin_name} §7- You do not have enough currency (§6{cost}§7) for this upgrade.")
                        .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                        .replace("{cost}", String.valueOf(currencyCost)));
                return true;
            }

            economy.withdrawPlayer(player, currencyCost);
            player.sendMessage(plugin.getMessage("commands.enhancedtools.currency-deducted", "{plugin_name} §7- Deducted §6{cost} currency§7 from your balance.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{cost}", String.valueOf(currencyCost)));
        } else {
            if (player.getLevel() < xpCost) {
                player.sendMessage(plugin.getMessage("commands.enhancedtools.not-enough-xp", "{plugin_name} §7- You do not have enough XP levels (§b{cost}§7).")
                        .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                        .replace("{cost}", String.valueOf(xpCost)));
                return true;
            }
            player.setLevel(player.getLevel() - xpCost);
            player.sendMessage(plugin.getMessage("commands.enhancedtools.xp-deducted", "{plugin_name} §7- Deducted §b{cost} XP levels§7.")
                    .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                    .replace("{cost}", String.valueOf(xpCost)));
        }

        ItemMeta meta = itemInHand.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        itemInHand.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.sendMessage(plugin.getMessage("commands.enhancedtools.upgrade-success", "{plugin_name} §aYour item has been upgraded to §e{enchant} {level}§a!")
                .replace("{plugin_name}", EnhancedTools.getFormattedPluginName())
                .replace("{enchant}", enchantName)
                .replace("{level}", String.valueOf(level)));

        return true;
    }

    private static final List<String> ENCHANTMENTS = List.of(
            "protection", "fire_protection", "feather_falling", "blast_protection",
            "projectile_protection", "respiration", "aqua_affinity", "thorns",
            "depth_strider", "frost_walker", "binding_curse", "soul_speed",
            "swift_sneak", "sharpness", "smite", "bane_of_arthropods",
            "knockback", "fire_aspect", "looting", "sweeping", "sweeping_edge",
            "efficiency", "silk_touch", "unbreaking", "fortune",
            "power", "punch", "flame", "infinity", "multishot",
            "quick_charge", "piercing", "channeling", "loyalty",
            "impaling", "riptide", "luck_of_the_sea", "lure",
            "mending", "vanishing_curse"
    );
    private Enchantment matchCustomEnchant(String name) {
        return switch (name.toLowerCase()) {
            case "protection" -> Enchantment.PROTECTION_ENVIRONMENTAL;
            case "fire_protection" -> Enchantment.PROTECTION_FIRE;
            case "feather_falling" -> Enchantment.PROTECTION_FALL;
            case "blast_protection" -> Enchantment.PROTECTION_EXPLOSIONS;
            case "projectile_protection" -> Enchantment.PROTECTION_PROJECTILE;
            case "respiration" -> Enchantment.OXYGEN;
            case "aqua_affinity" -> Enchantment.WATER_WORKER;
            case "thorns" -> Enchantment.THORNS;
            case "depth_strider" -> Enchantment.DEPTH_STRIDER;
            case "frost_walker" -> Enchantment.FROST_WALKER;
            case "binding_curse" -> Enchantment.BINDING_CURSE;
            case "soul_speed" -> Enchantment.SOUL_SPEED;
            case "sharpness" -> Enchantment.DAMAGE_ALL;
            case "smite" -> Enchantment.DAMAGE_UNDEAD;
            case "bane_of_arthropods" -> Enchantment.DAMAGE_ARTHROPODS;
            case "knockback" -> Enchantment.KNOCKBACK;
            case "fire_aspect" -> Enchantment.FIRE_ASPECT;
            case "looting" -> Enchantment.LOOT_BONUS_MOBS;
            case "sweeping_edge" -> Enchantment.SWEEPING_EDGE;
            case "efficiency" -> Enchantment.DIG_SPEED;
            case "silk_touch" -> Enchantment.SILK_TOUCH;
            case "unbreaking" -> Enchantment.DURABILITY;
            case "fortune" -> Enchantment.LOOT_BONUS_BLOCKS;
            case "power" -> Enchantment.ARROW_DAMAGE;
            case "punch" -> Enchantment.ARROW_KNOCKBACK;
            case "flame" -> Enchantment.ARROW_FIRE;
            case "infinity" -> Enchantment.ARROW_INFINITE;
            case "multishot" -> Enchantment.MULTISHOT;
            case "quick_charge" -> Enchantment.QUICK_CHARGE;
            case "piercing" -> Enchantment.PIERCING;
            case "channeling" -> Enchantment.CHANNELING;
            case "loyalty" -> Enchantment.LOYALTY;
            case "impaling" -> Enchantment.IMPALING;
            case "riptide" -> Enchantment.RIPTIDE;
            case "luck_of_the_sea" -> Enchantment.LUCK;
            case "lure" -> Enchantment.LURE;
            case "mending" -> Enchantment.MENDING;
            case "vanishing_curse" -> Enchantment.VANISHING_CURSE;
            default -> null; // Return null if no match
        };
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        // Add default enchantments if missing
        addMissingEnchantmentsToConfig();

        if (args.length == 1) {
            ConfigurationSection enchantsSection = plugin.getConfig().getConfigurationSection("enchants");
            if (enchantsSection != null) {
                return enchantsSection.getKeys(false).stream().toList();
            }
            plugin.getLogger().warning("Enchants section is null in configuration.");
            return List.of("protection", "sharpness"); // Default suggestions
        }

        if (args.length == 2) {
            String enchantName = args[0].toLowerCase();
            ConfigurationSection enchantConfig = plugin.getConfig().getConfigurationSection("enchants." + enchantName);
            if (enchantConfig != null && enchantConfig.contains("levels")) {
                ConfigurationSection levelsSection = enchantConfig.getConfigurationSection("levels");
                if (levelsSection != null) {
                    return levelsSection.getKeys(false).stream().toList();
                }
                plugin.getLogger().warning("Levels section is null for enchantment: " + enchantName);
            }
        }

        return Collections.emptyList();
    }

    private void addMissingEnchantmentsToConfig() {
        ConfigurationSection enchantsSection = plugin.getConfig().getConfigurationSection("enchants");
        if (enchantsSection == null) {
            enchantsSection = plugin.getConfig().createSection("enchants");
        }

        for (String enchantName : ENCHANTMENTS) {
            if (!enchantsSection.contains(enchantName)) {
                plugin.getLogger().info("Adding missing enchantment to config: " + enchantName);

                ConfigurationSection enchantConfig = enchantsSection.createSection(enchantName);
                enchantConfig.set("enabled", true);
                enchantConfig.set("use-xp", true);
                enchantConfig.set("use-currency", true);

                ConfigurationSection levelsSection = enchantConfig.createSection("levels");
                ConfigurationSection level1 = levelsSection.createSection("1");
                level1.set("xp-cost", 10);
                level1.set("currency-cost", 50);

                ConfigurationSection level2 = levelsSection.createSection("2");
                level2.set("xp-cost", 20);
                level2.set("currency-cost", 100);
            }
        }
        plugin.saveConfig();
    }
}