package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.MaterialUtil;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import java.util.ArrayList;
import java.util.Map;

import me.kapehh.main.pluginmanager.utils.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SkillRepair extends ActiveSkill {
    String useText = null;

    public SkillRepair(Heroes plugin) {
        super(plugin, "Repair");
        setDescription("You are able to repair tools and armor. There is a $1% chance the item will be disenchanted.");
        setUsage("/skill repair");
        setArgumentRange(0, 0);
        setIdentifiers("skill repair");
        setTypes(SkillType.ITEM, SkillType.PHYSICAL, SkillType.KNOWLEDGE);
    }

    public void init() {
        super.init();
        this.useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT, "%hero% repaired a %item%%ench%").replace("%hero%", "$1").replace("%item%", "$2").replace("%ench%", "$3");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.USE_TEXT.node(), "%hero% repaired a %item%%ench%");
        node.set("wood-weapons", Integer.valueOf(1));
        node.set("stone-weapons", Integer.valueOf(1));
        node.set("iron-weapons", Integer.valueOf(1));
        node.set("gold-weapons", Integer.valueOf(1));
        node.set("diamond-weapons", Integer.valueOf(1));
        node.set("leather-armor", Integer.valueOf(1));
        node.set("iron-armor", Integer.valueOf(1));
        node.set("chain-armor", Integer.valueOf(1));
        node.set("gold-armor", Integer.valueOf(1));
        node.set("diamond-armor", Integer.valueOf(1));
        node.set("wood-tools", Integer.valueOf(1));
        node.set("stone-tools", Integer.valueOf(1));
        node.set("iron-tools", Integer.valueOf(1));
        node.set("gold-tools", Integer.valueOf(1));
        node.set("diamond-tools", Integer.valueOf(1));
        node.set("fishing-rod", Integer.valueOf(1));
        node.set("shears", Integer.valueOf(1));
        node.set("flint-steel", Integer.valueOf(1));
        node.set("unchant-chance", Double.valueOf(0.5D));
        node.set("unchant-chance-reduce", Double.valueOf(0.005D));
        return node;
    }

    private int getRepairCost(ItemStack is) {
        Material mat = is.getType();
        int amt;
        switch (mat) {
            case BOW:
                amt = (int) (is.getDurability() / mat.getMaxDurability() * 2.0D);
                return amt < 1 ? 1 : amt;
            case LEATHER_BOOTS:
            case IRON_BOOTS:
            case CHAINMAIL_BOOTS:
            case GOLD_BOOTS:
            case DIAMOND_BOOTS:
                amt = (int) (is.getDurability() / mat.getMaxDurability() * 3.0D);
                return amt < 1 ? 1 : amt;
            case LEATHER_HELMET:
            case IRON_HELMET:
            case CHAINMAIL_HELMET:
            case GOLD_HELMET:
            case DIAMOND_HELMET:
                amt = (int) (is.getDurability() / mat.getMaxDurability() * 4.0D);
                return amt < 1 ? 1 : amt;
            case LEATHER_CHESTPLATE:
            case IRON_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case GOLD_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
                amt = (int) (is.getDurability() / mat.getMaxDurability() * 7.0D);
                return amt < 1 ? 1 : amt;
            case LEATHER_LEGGINGS:
            case IRON_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case GOLD_LEGGINGS:
            case DIAMOND_LEGGINGS:
                amt = (int) (is.getDurability() / mat.getMaxDurability() * 6.0D);
                return amt < 1 ? 1 : amt;
        }
        return 1;
    }

    private int getRequiredLevel(Hero hero, Material material) {
        switch (material) {
            case BOW:
            case WOOD_SWORD:
            case WOOD_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "wood-weapons", 1, true);
            case WOOD_HOE:
            case WOOD_PICKAXE:
            case WOOD_SPADE:
                return SkillConfigManager.getUseSetting(hero, this, "wood-tools", 1, true);
            case STONE_SWORD:
            case STONE_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "stone-weapons", 1, true);
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SPADE:
                return SkillConfigManager.getUseSetting(hero, this, "stone-tools", 1, true);
            case SHEARS:
                return SkillConfigManager.getUseSetting(hero, this, "shears", 1, true);
            case FLINT_AND_STEEL:
                return SkillConfigManager.getUseSetting(hero, this, "flint-steel", 1, true);
            case IRON_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "iron-armor", 1, true);
            case IRON_SWORD:
            case IRON_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "iron-weapons", 1, true);
            case IRON_HOE:
            case IRON_PICKAXE:
            case IRON_SPADE:
                return SkillConfigManager.getUseSetting(hero, this, "iron-tools", 1, true);
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "chain-armor", 1, true);
            case GOLD_BOOTS:
            case GOLD_HELMET:
            case GOLD_CHESTPLATE:
            case GOLD_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "gold-armor", 1, true);
            case GOLD_SWORD:
            case GOLD_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "gold-weapons", 1, true);
            case GOLD_HOE:
            case GOLD_PICKAXE:
            case GOLD_SPADE:
                return SkillConfigManager.getUseSetting(hero, this, "gold-tools", 1, true);
            case DIAMOND_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-armor", 1, true);
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-weapons", 1, true);
            case DIAMOND_HOE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SPADE:
                return SkillConfigManager.getUseSetting(hero, this, "diamond-tools", 1, true);
            case LEATHER_BOOTS:
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
                return SkillConfigManager.getUseSetting(hero, this, "leather-armor", 1, true);
            case FISHING_ROD:
                return SkillConfigManager.getUseSetting(hero, this, "fishing-rod", 1, true);
        }
        return -1;
    }

    private Material getRequiredReagent(Material material) {
        switch (material) {
            case WOOD_SWORD:
            case WOOD_AXE:
            case WOOD_HOE:
            case WOOD_PICKAXE:
            case WOOD_SPADE:
                return Material.WOOD;
            case STONE_SWORD:
            case STONE_AXE:
            case STONE_HOE:
            case STONE_PICKAXE:
            case STONE_SPADE:
                return Material.COBBLESTONE;
            case IRON_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case SHEARS:
            case FLINT_AND_STEEL:
            case IRON_SWORD:
            case IRON_AXE:
            case IRON_HOE:
            case IRON_PICKAXE:
            case IRON_SPADE:
                return Material.IRON_INGOT;
            case GOLD_BOOTS:
            case GOLD_HELMET:
            case GOLD_CHESTPLATE:
            case GOLD_LEGGINGS:
            case GOLD_SWORD:
            case GOLD_AXE:
            case GOLD_HOE:
            case GOLD_PICKAXE:
            case GOLD_SPADE:
                return Material.GOLD_INGOT;
            case DIAMOND_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
            case DIAMOND_HOE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SPADE:
                return Material.DIAMOND;
            case LEATHER_BOOTS:
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
                return Material.LEATHER;
            case BOW:
            case FISHING_ROD:
                return Material.STRING;
            case CHAINMAIL_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
                return Material.IRON_FENCE;
        }
        return null;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        ItemStack is = player.getItemInHand();
        Material isType = is.getType();
        int level = getRequiredLevel(hero, isType);
        Material reagent = getRequiredReagent(isType);
        if ((level == -1) || (reagent == null)) {
            Messaging.send(player, "You are not holding a repairable tool.");
            return SkillResult.FAIL;
        }
        if (hero.getSkillLevel(this) < level) {
            Messaging.send(player, "You must be level $1 to repair $2", Integer.valueOf(level), MaterialUtil.getFriendlyName(isType));
            return new SkillResult(SkillResult.ResultType.LOW_LEVEL, false);
        }
        if (is.getDurability() == 0) {
            Messaging.send(player, "That item is already at full durability!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        ItemStack reagentStack = new ItemStack(reagent, getRepairCost(is));
        //if (!hasReagentCost(player, reagentStack)) {
        if (!PlayerUtil.isContainsItem(player.getInventory(), reagentStack)) {
            return new SkillResult(SkillResult.ResultType.MISSING_REAGENT, true, Integer.valueOf(reagentStack.getAmount()), MaterialUtil.getFriendlyName(reagentStack.getType()));
        }
        boolean lost = false;
        boolean enchanted = !is.getEnchantments().isEmpty();
        if (enchanted) {
            double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", 0.5D, true);
            unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", 0.005D, false) * hero.getSkillLevel(this);
            if (Util.nextRand() <= unchant) {
                for (Enchantment enchant : is.getEnchantments().keySet()) {
                    is.removeEnchantment(enchant);
                }
                lost = true;
            }
        }
        /*if (!player.getInventory().contains(reagentStack)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }*/
        //player.getInventory().removeItem(reagentStack);
        is.setDurability((short) 0);
        Util.syncInventory(player, this.plugin);
        PlayerUtil.takeItems(player.getInventory(), reagentStack);
        broadcast(player.getLocation(), this.useText, player.getDisplayName(), is.getType().name().toLowerCase().replace("_", " "), lost ? " and stripped it of enchantments!" : !enchanted ? "." : " and successfully kept the enchantments.");
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        double unchant = SkillConfigManager.getUseSetting(hero, this, "unchant-chance", 0.5D, true);
        unchant -= SkillConfigManager.getUseSetting(hero, this, "unchant-chance-reduce", 0.005D, false) * hero.getSkillLevel(this);
        return getDescription().replace("$1", Util.stringDouble(unchant * 100.0D));
    }
}
