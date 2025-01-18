package me.swishhyy.enhancedtools.commands;

import me.swishhyy.enhancedtools.EnhancedTools;
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

    // Reference to your main plugin so we can call plugin.getConfig()
    private final EnhancedTools plugin;

    public UpgradeCommand(EnhancedTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // 1) Only players
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        // 2) Item check
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("You're not holding any item to upgrade!");
            return true;
        }

        // 3) Require 2 arguments: /upgrade <enchant> <level>
        if (args.length < 2) {
            player.sendMessage("Usage: /upgrade <enchant> <level>");
            return true;
        }
        String enchantName = args[0].toLowerCase();
        String levelString = args[1];

        // 4) Parse desired level
        int level;
        try {
            level = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid level! Must be a number.");
            return true;
        }

        // 5) Convert user-friendly name -> Bukkit Enchantment
        Enchantment enchantment = matchCustomEnchant(enchantName);
        if (enchantment == null) {
            player.sendMessage("Unknown enchantment: " + enchantName);
            return true;
        }

        // 6) Check if enchant is compatible with the item
        if (!enchantment.canEnchantItem(itemInHand)) {
            player.sendMessage("You cannot apply " + enchantName + " to " + itemInHand.getType() + "!");
            return true;
        }

        // 7) Load config data for this enchant: max-level, cost, cost-scale
        ConfigurationSection enchantsSection = plugin.getConfig().getConfigurationSection("enchants");
        if (enchantsSection == null) {
            player.sendMessage("The config is missing an 'enchants' section!");
            return true;
        }

        // Each enchant in config is stored by its key, e.g. "sharpness", "efficiency"
        if (!enchantsSection.contains(enchantName)) {
            player.sendMessage("Config doesn't have settings for " + enchantName + "!");
            return true;
        }

        ConfigurationSection enchantConfig = enchantsSection.getConfigurationSection(enchantName);
        // Check if the configuration section exists
        if (enchantConfig == null) {
            player.sendMessage("The configuration for " + enchantName + " is invalid or missing!");
            return true; // Exit the command if the section is null
        }
        int maxLevel = enchantConfig.getInt("max-level", 5);         // default 5 if not in config
        double baseCost = enchantConfig.getDouble("base-cost", 5.0); // default 5.0
        double costScale = enchantConfig.getDouble("cost-scale", 1.0);

        // Enforce max-level
        if (level > maxLevel) {
            player.sendMessage("That enchantment is capped at level " + maxLevel + "!");
            return true;
        }

        // 8) Calculate cost
        // Example formula: cost = baseCost * (costScale^(level - 1))
        double finalCost = baseCost * Math.pow(costScale, (level - 1));

        // 9) Check if we use Vault or XP, then subtract cost
        boolean useVault = plugin.getConfig().getBoolean("use-vault", false);
        if (useVault) {
            // PSEUDO-CODE: If you have an economy reference from Vault:
            //    if (economy.getBalance(player) < finalCost) {
            //        player.sendMessage("You don't have enough money!");
            //        return true;
            //    }
            //    economy.withdrawPlayer(player, finalCost);
            // For now, let's just pretend we do it:
            player.sendMessage("Cost " + finalCost + " currency (Vault) - pseudo-charged!");
        } else {
            // XP approach. This is one possibility:
            // 1) check if player's current level >= finalCost
            //    if not, "Not enough XP!"
            // 2) subtract from player's XP levels:
            if (player.getLevel() < finalCost) {
                player.sendMessage("You don't have enough XP levels!");
                return true;
            }
            // Downcast if finalCost is float/double. Usually you'd do an int cost or rework the formula.
            player.setLevel((int) (player.getLevel() - finalCost));
            player.sendMessage("Cost " + finalCost + " XP levels - subtracted!");
        }

        // 10) Apply the enchant ignoring vanilla level limits
        ItemMeta meta = itemInHand.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        itemInHand.setItemMeta(meta);

        // 11) Play a sound and notify
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        // Optionally use a message from config with placeholders:
        String successMsg = plugin.getConfig().getString("messages.upgrade-success",
                "Your item was upgraded to {ENCHANT} {LEVEL}!");
        successMsg = successMsg
                .replace("{ENCHANT}", enchantName)
                .replace("{LEVEL}", String.valueOf(level));
        player.sendMessage(successMsg);

        return true;
    }


    private Enchantment matchCustomEnchant(String name) {
        return switch (name.toLowerCase()) {

            // Armor Protections
            case "protection" -> Enchantment.PROTECTION_ENVIRONMENTAL;
            case "fire_protection" -> Enchantment.PROTECTION_FIRE;
            case "feather_falling" -> Enchantment.PROTECTION_FALL;
            case "blast_protection" -> Enchantment.PROTECTION_EXPLOSIONS;
            case "projectile_protection" -> Enchantment.PROTECTION_PROJECTILE;

            // Armor/Helmet
            case "respiration" -> Enchantment.OXYGEN;
            case "aqua_affinity", "water_worker" -> Enchantment.WATER_WORKER;
            case "thorns" -> Enchantment.THORNS;
            case "depth_strider" -> Enchantment.DEPTH_STRIDER;
            case "frost_walker" -> Enchantment.FROST_WALKER;
            case "binding_curse" -> Enchantment.BINDING_CURSE;
            case "soul_speed" -> Enchantment.SOUL_SPEED;
            case "swift_sneak" -> Enchantment.SWIFT_SNEAK;

            // Weapons
            case "sharpness" -> Enchantment.DAMAGE_ALL;
            case "smite" -> Enchantment.DAMAGE_UNDEAD;
            case "bane_of_arthropods", "arthropods" -> Enchantment.DAMAGE_ARTHROPODS;
            case "knockback" -> Enchantment.KNOCKBACK;
            case "fire_aspect" -> Enchantment.FIRE_ASPECT;
            case "looting" -> Enchantment.LOOT_BONUS_MOBS;
            case "sweeping", "sweeping_edge" -> Enchantment.SWEEPING_EDGE;

            // Tools
            case "efficiency" -> Enchantment.DIG_SPEED;
            case "silk_touch" -> Enchantment.SILK_TOUCH;
            case "unbreaking" -> Enchantment.DURABILITY;
            case "fortune" -> Enchantment.LOOT_BONUS_BLOCKS;

            // Bows & Crossbows
            case "power" -> Enchantment.ARROW_DAMAGE;
            case "punch" -> Enchantment.ARROW_KNOCKBACK;
            case "flame" -> Enchantment.ARROW_FIRE;
            case "infinity" -> Enchantment.ARROW_INFINITE;
            case "multishot" -> Enchantment.MULTISHOT;
            case "quick_charge" -> Enchantment.QUICK_CHARGE;
            case "piercing" -> Enchantment.PIERCING;

            // Tridents
            case "channeling" -> Enchantment.CHANNELING;
            case "loyalty" -> Enchantment.LOYALTY;
            case "impaling" -> Enchantment.IMPALING;
            case "riptide" -> Enchantment.RIPTIDE;

            // Fishing Rod
            case "luck", "luck_of_the_sea" -> Enchantment.LUCK;
            case "lure" -> Enchantment.LURE;

            // Misc
            case "mending" -> Enchantment.MENDING;
            case "vanishing_curse" -> Enchantment.VANISHING_CURSE;

            // If none match, return null
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        // Return an empty list if you don't have suggestions
        return Collections.emptyList();
    }
}