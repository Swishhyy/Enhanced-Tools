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
            sender.sendMessage(plugin.getMessage("only-players", "Only players can use this command!"));
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessage("holding-air", "You're not holding an item to upgrade!"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("invalid-usage", "Usage: /upgrade <enchant> <level>"));
            return true;
        }

        String enchantName = args[0].toLowerCase();
        String levelString = args[1];

        int level;
        try {
            level = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("invalid-level", "Invalid level! Must be a number."));
            return true;
        }

        Enchantment enchantment = matchCustomEnchant(enchantName);
        if (enchantment == null) {
            player.sendMessage(plugin.getMessage("unknown-enchantment", "Unknown enchantment: {enchant}")
                    .replace("{enchant}", enchantName));
            return true;
        }

        if (!enchantment.canEnchantItem(itemInHand)) {
            player.sendMessage(plugin.getMessage("cannot-apply", "{enchant} cannot be applied to {item}")
                    .replace("{enchant}", enchantName)
                    .replace("{item}", itemInHand.getType().toString()));
            return true;
        }

        ConfigurationSection enchantsSection = plugin.getConfig().getConfigurationSection("enchants");
        if (enchantsSection == null) {
            player.sendMessage(plugin.getMessage("missing-enchants-section", "Configuration error: Missing 'enchants' section."));
            return true;
        }

        if (!enchantsSection.contains(enchantName)) {
            player.sendMessage(plugin.getMessage("missing-enchant-config", "No configuration found for enchantment {enchant}.")
                    .replace("{enchant}", enchantName));
            return true;
        }

        ConfigurationSection enchantConfig = enchantsSection.getConfigurationSection(enchantName);
        if (enchantConfig == null || !enchantConfig.getBoolean("enabled", true)) {
            player.sendMessage(plugin.getMessage("not-enabled", "{enchant} is not enabled or cannot be upgraded!")
                    .replace("{enchant}", enchantName));
            return true;
        }

        if (!enchantConfig.contains("levels")) {
            player.sendMessage(plugin.getMessage("missing-upgrade-levels", "{enchant} has no upgrade levels defined.")
                    .replace("{enchant}", enchantName));
            return true;
        }

        ConfigurationSection levelConfig = enchantConfig.getConfigurationSection("levels." + level);
        if (levelConfig == null) {
            player.sendMessage(plugin.getMessage("invalid-upgrade-level", "Invalid level {level} for enchantment {enchant}.")
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
                player.sendMessage(plugin.getMessage("not-enough-currency", "You do not have enough currency ({cost}) for this upgrade.")
                        .replace("{cost}", String.valueOf(currencyCost)));
                return true;
            }

            economy.withdrawPlayer(player, currencyCost);
            player.sendMessage(plugin.getMessage("currency-deducted", "Deducted {cost} currency from your balance.")
                    .replace("{cost}", String.valueOf(currencyCost)));
        } else {
            if (player.getLevel() < xpCost) {
                player.sendMessage(plugin.getMessage("not-enough-xp", "You do not have enough XP levels ({cost}).")
                        .replace("{cost}", String.valueOf(xpCost)));
                return true;
            }
            player.setLevel(player.getLevel() - xpCost);
            player.sendMessage(plugin.getMessage("xp-deducted", "Deducted {cost} XP levels.")
                    .replace("{cost}", String.valueOf(xpCost)));
        }

        ItemMeta meta = itemInHand.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        itemInHand.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        String successMsg = plugin.getMessage("upgrade-success", "Your item has been upgraded to {enchant} {level}!");
        player.sendMessage(successMsg.replace("{enchant}", enchantName).replace("{level}", String.valueOf(level)));

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