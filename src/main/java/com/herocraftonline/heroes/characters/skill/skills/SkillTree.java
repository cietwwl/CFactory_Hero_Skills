package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.herocraftonline.heroes.util.Messaging;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import me.kapehh.main.pluginmanager.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillTree extends ActiveSkill {
    public SkillTree(Heroes plugin) {
        super(plugin, "Tree");
        this.setDescription("Create a small tree temporarily. R:$1");
        this.setUsage("/skill tree");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill tree");
        this.setTypes(SkillType.EARTH, SkillType.SILENCABLE, SkillType.SUMMON);
    }

    public String getDescription(Hero hero) {
        int distance = (int) ((double) SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 100, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * (double) hero.getSkillLevel(this));
        distance = distance > 0 ? distance : 0;
        String description = this.getDescription().replace("$1", distance + "");
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description = description + " CD:" + cooldown + "s";
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        if (mana > 0) {
            description = description + " M:" + mana;
        }

        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this);
        if (healthCost > 0) {
            description = description + " HP:" + healthCost;
        }

        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);
        if (staminaCost > 0) {
            description = description + " FP:" + staminaCost;
        }

        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description = description + " W:" + delay + "s";
        }

        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description = description + " XP:" + exp;
        }

        return description;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 100);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        return node;
    }

    private boolean checkMyPlot(Player player, Location location) {
        TownBlock townBlock = TownyUniverse.getTownBlock(location);
        if (townBlock == null)
            return false;

        if (!townBlock.hasTown() || !townBlock.hasResident())
            return false;

        try {
            return player.getName().equalsIgnoreCase(townBlock.getResident().getName());
        } catch (NotRegisteredException e) {
            return false;
        }
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int distance = (int) ((double) SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 100, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * (double) hero.getSkillLevel(this));
        distance = distance > 0 ? distance : 0;

        Block refBlock = player.getTargetBlock((Set<Material>) null, distance);
        Block refBlockTop = refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.UP);

        Block blockBotNorthEast = refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.NORTH_EAST);
        Block blockBotNorthWest = refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.NORTH_WEST);
        Block blockBotSouthEast = refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.SOUTH_EAST);
        Block blockBotSouthWest = refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.SOUTH_WEST);
        Block blockTopNorthEast = refBlockTop.getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.NORTH_EAST);
        Block blockTopNorthWest = refBlockTop.getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.NORTH_WEST);
        Block blockTopSouthEast = refBlockTop.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.SOUTH_EAST);
        Block blockTopSouthWest = refBlockTop.getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.SOUTH_WEST);

        // Проверка, является ли плот во владениях игрока
        if (!checkMyPlot(player, blockBotNorthEast.getLocation()) ||
                !checkMyPlot(player, blockBotNorthWest.getLocation()) ||
                !checkMyPlot(player, blockBotSouthEast.getLocation()) ||
                !checkMyPlot(player, blockBotSouthWest.getLocation())) {
            Messaging.send(player, "You can't build here!");
            return SkillResult.CANCELLED;
        }

        Material refMatNorthEast = blockTopNorthEast.getType();
        Material refMatNorthWest = blockTopNorthWest.getType();
        Material refMatSouthEast = blockTopSouthEast.getType();
        Material refMatSouthWest = blockTopSouthWest.getType();

        if (refBlock.getRelative(BlockFace.UP).getType() == Material.AIR &&
                (refBlock.getType() == Material.DIRT || refBlock.getType() == Material.GRASS) &&
                refMatNorthEast == Material.AIR &&
                refMatNorthWest == Material.AIR &&
                refMatSouthEast == Material.AIR &&
                refMatSouthWest == Material.AIR &&
                refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH_EAST).getType() == Material.AIR &&
                refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH_WEST).getType() == Material.AIR &&
                refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH_EAST).getType() == Material.AIR &&
                refBlock.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH_WEST).getType() == Material.AIR) {

            final Block wTargetBlock = player.getTargetBlock((Set<Material>) null, 100).getRelative(BlockFace.UP);
            final Block wOneUp = wTargetBlock.getRelative(BlockFace.UP);
            final Block wTwoUp = wOneUp.getRelative(BlockFace.UP);
            final Block wThreeUp = wTwoUp.getRelative(BlockFace.UP);
            final Block wFourUp = wThreeUp.getRelative(BlockFace.UP);
            final Block wOneNorthUp = wOneUp.getRelative(BlockFace.NORTH);
            final Block wOneSouthUp = wOneUp.getRelative(BlockFace.SOUTH);
            final Block wOneEastUp = wOneUp.getRelative(BlockFace.EAST);
            final Block wOneWestUp = wOneUp.getRelative(BlockFace.WEST);
            final Block wTwoNorthUp = wTwoUp.getRelative(BlockFace.NORTH);
            final Block wTwoSouthUp = wTwoUp.getRelative(BlockFace.SOUTH);
            final Block wTwoEastUp = wTwoUp.getRelative(BlockFace.EAST);
            final Block wTwoWestUp = wTwoUp.getRelative(BlockFace.WEST);
            final ArrayList<Material> matList = new ArrayList<Material>();
            matList.add(wTargetBlock.getType());
            matList.add(wOneUp.getType());
            matList.add(wOneNorthUp.getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.NORTH).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.NORTH_EAST).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.NORTH_WEST).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.EAST).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.WEST).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.EAST).getType());
            matList.add(wOneNorthUp.getRelative(BlockFace.WEST).getType());
            matList.add(wOneSouthUp.getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.SOUTH).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.SOUTH_EAST).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.SOUTH_WEST).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.EAST).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.WEST).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.EAST).getType());
            matList.add(wOneSouthUp.getRelative(BlockFace.WEST).getType());
            matList.add(wOneEastUp.getType());
            matList.add(wOneEastUp.getRelative(BlockFace.EAST).getType());
            matList.add(wOneEastUp.getRelative(BlockFace.NORTH_EAST).getType());
            matList.add(wOneEastUp.getRelative(BlockFace.SOUTH_EAST).getType());
            matList.add(wOneWestUp.getType());
            matList.add(wOneWestUp.getRelative(BlockFace.WEST).getType());
            matList.add(wOneWestUp.getRelative(BlockFace.NORTH_WEST).getType());
            matList.add(wOneWestUp.getRelative(BlockFace.SOUTH_WEST).getType());
            matList.add(wTwoUp.getType());
            matList.add(wTwoNorthUp.getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.NORTH).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.NORTH_EAST).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.NORTH_WEST).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.EAST).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.WEST).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.EAST).getType());
            matList.add(wTwoNorthUp.getRelative(BlockFace.WEST).getType());
            matList.add(wTwoSouthUp.getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.SOUTH).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.SOUTH_EAST).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.SOUTH_WEST).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.EAST).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.WEST).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.EAST).getType());
            matList.add(wTwoSouthUp.getRelative(BlockFace.WEST).getType());
            matList.add(wTwoEastUp.getType());
            matList.add(wTwoEastUp.getRelative(BlockFace.EAST).getType());
            matList.add(wTwoEastUp.getRelative(BlockFace.NORTH_EAST).getType());
            matList.add(wTwoEastUp.getRelative(BlockFace.SOUTH_EAST).getType());
            matList.add(wTwoWestUp.getType());
            matList.add(wTwoWestUp.getRelative(BlockFace.WEST).getType());
            matList.add(wTwoWestUp.getRelative(BlockFace.NORTH_WEST).getType());
            matList.add(wTwoWestUp.getRelative(BlockFace.SOUTH_WEST).getType());
            matList.add(wThreeUp.getType());
            matList.add(wThreeUp.getRelative(BlockFace.NORTH).getType());
            matList.add(wThreeUp.getRelative(BlockFace.NORTH_EAST).getType());
            matList.add(wThreeUp.getRelative(BlockFace.SOUTH).getType());
            matList.add(wThreeUp.getRelative(BlockFace.EAST).getType());
            matList.add(wThreeUp.getRelative(BlockFace.WEST).getType());
            matList.add(wFourUp.getType());
            matList.add(wFourUp.getRelative(BlockFace.NORTH).getType());
            matList.add(wFourUp.getRelative(BlockFace.SOUTH).getType());
            matList.add(wFourUp.getRelative(BlockFace.EAST).getType());
            matList.add(wFourUp.getRelative(BlockFace.WEST).getType());
            wTargetBlock.setType(Material.SAPLING);
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                public void run() {
                    wTargetBlock.setType(Material.LOG);
                    wOneUp.setType(Material.LOG);
                    wOneNorthUp.setType(Material.LEAVES);
                    wOneSouthUp.setType(Material.LEAVES);
                    wOneEastUp.setType(Material.LEAVES);
                    wOneWestUp.setType(Material.LEAVES);
                    wTwoUp.setType(Material.LEAVES);
                    SkillTree.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(SkillTree.this.plugin, new Runnable() {
                        public void run() {
                            wOneNorthUp.getRelative(BlockFace.NORTH).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.NORTH_EAST).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.NORTH_WEST).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wOneNorthUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.SOUTH).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.SOUTH_EAST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.SOUTH_WEST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wOneSouthUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wOneEastUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wOneEastUp.getRelative(BlockFace.NORTH_EAST).setType(Material.LEAVES);
                            wOneEastUp.getRelative(BlockFace.SOUTH_EAST).setType(Material.LEAVES);
                            wOneWestUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wOneWestUp.getRelative(BlockFace.NORTH_WEST).setType(Material.LEAVES);
                            wOneWestUp.getRelative(BlockFace.SOUTH_WEST).setType(Material.LEAVES);
                            wTwoUp.setType(Material.LOG);
                            wTwoNorthUp.setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.NORTH).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.NORTH_EAST).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.NORTH_WEST).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.NORTH_EAST).getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.NORTH_WEST).getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wTwoNorthUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wTwoSouthUp.setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.SOUTH).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.SOUTH_EAST).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.SOUTH_WEST).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.SOUTH_WEST).getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wTwoSouthUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wTwoEastUp.setType(Material.LEAVES);
                            wTwoEastUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wTwoEastUp.getRelative(BlockFace.NORTH_EAST).setType(Material.LEAVES);
                            wTwoEastUp.getRelative(BlockFace.SOUTH_EAST).setType(Material.LEAVES);
                            wTwoWestUp.setType(Material.LEAVES);
                            wTwoWestUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wTwoWestUp.getRelative(BlockFace.NORTH_WEST).setType(Material.LEAVES);
                            wTwoWestUp.getRelative(BlockFace.SOUTH_WEST).setType(Material.LEAVES);
                            wThreeUp.setType(Material.LOG);
                            wThreeUp.getRelative(BlockFace.NORTH).setType(Material.LEAVES);
                            wThreeUp.getRelative(BlockFace.NORTH_EAST).setType(Material.LEAVES);
                            wThreeUp.getRelative(BlockFace.SOUTH).setType(Material.LEAVES);
                            wThreeUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wThreeUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                            wFourUp.setType(Material.LEAVES);
                            wFourUp.getRelative(BlockFace.NORTH).setType(Material.LEAVES);
                            wFourUp.getRelative(BlockFace.SOUTH).setType(Material.LEAVES);
                            wFourUp.getRelative(BlockFace.EAST).setType(Material.LEAVES);
                            wFourUp.getRelative(BlockFace.WEST).setType(Material.LEAVES);
                        }
                    }, 6L);
                }
            }, 4L);
            return SkillResult.NORMAL;
        } else {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}
