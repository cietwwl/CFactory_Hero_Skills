package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import me.kapehh.main.pluginmanager.utils.LocationUtil;
import me.kapehh.main.pluginmanager.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;

public class SkillFirewall extends ActiveSkill {
    public final static int MAX_DISTANCE = 120;

    public SkillFirewall(Heroes plugin) {
        super(plugin, "Firewall");
        setDescription("Create a wall of fire lasting $2-$3s. R:$1");
        setUsage("/skill firewall");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill firewall"});

        setTypes(SkillType.FIRE, SkillType.DAMAGING, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0, false) * hero.getSkillLevel(this));
        int minDuration = SkillConfigManager.getUseSetting(hero, this, "min-duration", 5000, false) / 1000;
        int maxDuration = SkillConfigManager.getUseSetting(hero, this, "max-duration", 10000, false) / 1000;
        String description = getDescription().replace("$1", distance + "").replace("$2", minDuration + "").replace("$3", maxDuration + "");

        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) -
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 25);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0);
        node.set("max-duration", 10000);
        node.set("min-duration", 5000);
        node.set("max-length", 9);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int range = Math.abs((int) SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 25, false));
        if (!(range > 0 && range <= 100)) {
            range = 25;
        }
        final Block wTargetBlock = player.getTargetBlock((Set<Material>) null, range);
        final Material matOne = wTargetBlock.getRelative(BlockFace.UP).getType();
        final Block wOneUp = wTargetBlock.getRelative(BlockFace.UP);
        final BlockFace wDirection = PlayerUtil.playerFace(player);

        // Если запрещено пвп, то не даем использование
        if (!LocationUtil.isLocationPVP(wOneUp.getLocation())) {
            Messaging.send(player, "This territory is NoPVP!");
            return SkillResult.CANCELLED;
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.equals(player) && p.getWorld().equals(wTargetBlock.getWorld()) && Math.sqrt(p.getLocation().distanceSquared(wTargetBlock.getLocation())) <= 6
                    && !damageCheck(p, player))
                return SkillResult.INVALID_TARGET;
        }

        int addedduration = Math.abs((int) (SkillConfigManager.getUseSetting(hero, this, "max-duration", 10000, false) / 1000) -
                (SkillConfigManager.getUseSetting(hero, this, "min-duration", 5000, false) / 1000));
        if (!(addedduration > 0 && addedduration <= 600)) {
            addedduration = 5;
        }
        int minDuration = Math.abs((int) (SkillConfigManager.getUseSetting(hero, this, "min-duration", 5000, false) / 1000));
        if (!(minDuration > 0 && minDuration <= 300)) {
            minDuration = 5;
        }
        long duration = (long) (Math.rint(Math.random() * addedduration) * 20 + 20 * minDuration);

        if (wDirection == BlockFace.NORTH || wDirection == BlockFace.SOUTH) {
            final Material matTwo   = retrieveBlock(wOneUp, 1, 0, 0).getType();
            final Material matThree = retrieveBlock(wOneUp, 2, 0, 0).getType();
            final Material matFour  = retrieveBlock(wOneUp, 3, 0, 0).getType();
            final Material matFive  = retrieveBlock(wOneUp, 4, 0, 0).getType();

            final Material matSix   = retrieveBlock(wOneUp, -1, 0, 0).getType();
            final Material matSeven = retrieveBlock(wOneUp, -2, 0, 0).getType();
            final Material matEight = retrieveBlock(wOneUp, -3, 0, 0).getType();
            final Material matNine  = retrieveBlock(wOneUp, -4, 0, 0).getType();

            if (retrieveBlock(wTargetBlock, 0, 0, 2).getType() == Material.AIR && matOne.equals(Material.AIR)) {
                wOneUp.setType(Material.FIRE);
                final int settedBlocks = setRelativeBlocks(player, wOneUp, wDirection, Material.FIRE, 3);

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (matOne.equals(Material.AIR)) wOneUp.setType(matOne);

                        if (wDirection.equals(BlockFace.NORTH)) {
                            if (settedBlocks <= 3 && matTwo.equals(Material.AIR))   retrieveBlock(wOneUp, 1, 0, 0).setType(matTwo);
                            if (settedBlocks <= 2 && matThree.equals(Material.AIR)) retrieveBlock(wOneUp, 2, 0, 0).setType(matThree);
                            if (settedBlocks <= 1 && matFour.equals(Material.AIR))  retrieveBlock(wOneUp, 3, 0, 0).setType(matFour);
                            if (settedBlocks <= 0 && matFive.equals(Material.AIR))  retrieveBlock(wOneUp, 4, 0, 0).setType(matFive);
                        } else if (wDirection.equals(BlockFace.SOUTH)) {
                            if (settedBlocks <= 3 && matSix.equals(Material.AIR))   retrieveBlock(wOneUp, -1, 0, 0).setType(matSix);
                            if (settedBlocks <= 2 && matSeven.equals(Material.AIR)) retrieveBlock(wOneUp, -2, 0, 0).setType(matSeven);
                            if (settedBlocks <= 1 && matEight.equals(Material.AIR)) retrieveBlock(wOneUp, -3, 0, 0).setType(matEight);
                            if (settedBlocks <= 0 && matNine.equals(Material.AIR))  retrieveBlock(wOneUp, -4, 0, 0).setType(matNine);
                        }
                    }
                }, duration);

            } else {
                Messaging.send(player, "No place!");
                return SkillResult.CANCELLED;
            }

        } else if (wDirection == BlockFace.EAST || wDirection == BlockFace.WEST) {
            final Material matTwo   = retrieveBlock(wOneUp, 0, 1, 0).getType();
            final Material matThree = retrieveBlock(wOneUp, 0, 2, 0).getType();
            final Material matFour  = retrieveBlock(wOneUp, 0, 3, 0).getType();
            final Material matFive  = retrieveBlock(wOneUp, 0, 4, 0).getType();

            final Material matSix   = retrieveBlock(wOneUp, 0, -1, 0).getType();
            final Material matSeven = retrieveBlock(wOneUp, 0, -2, 0).getType();
            final Material matEight = retrieveBlock(wOneUp, 0, -3, 0).getType();
            final Material matNine  = retrieveBlock(wOneUp, 0, -4, 0).getType();

            if (retrieveBlock(wTargetBlock, 0, 0, 2).getType() == Material.AIR && matOne.equals(Material.AIR)) {
                wOneUp.setType(Material.FIRE);
                final int settedBlocks = setRelativeBlocks(player, wOneUp, wDirection, Material.FIRE, 3);

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (matOne.equals(Material.AIR)) wOneUp.setType(matOne);

                        if (wDirection.equals(BlockFace.EAST)) {
                            if (settedBlocks <= 3 && matTwo.equals(Material.AIR))   retrieveBlock(wOneUp, 0, 1, 0).setType(matTwo);
                            if (settedBlocks <= 2 && matThree.equals(Material.AIR)) retrieveBlock(wOneUp, 0, 2, 0).setType(matThree);
                            if (settedBlocks <= 1 && matFour.equals(Material.AIR))  retrieveBlock(wOneUp, 0, 3, 0).setType(matFour);
                            if (settedBlocks <= 0 && matFive.equals(Material.AIR))  retrieveBlock(wOneUp, 0, 4, 0).setType(matFive);
                        } else if (wDirection.equals(BlockFace.WEST)) {
                            if (settedBlocks <= 3 && matSix.equals(Material.AIR))   retrieveBlock(wOneUp, 0, -1, 0).setType(matSix);
                            if (settedBlocks <= 2 && matSeven.equals(Material.AIR)) retrieveBlock(wOneUp, 0, -2, 0).setType(matSeven);
                            if (settedBlocks <= 1 && matEight.equals(Material.AIR)) retrieveBlock(wOneUp, 0, -3, 0).setType(matEight);
                            if (settedBlocks <= 0 && matNine.equals(Material.AIR))  retrieveBlock(wOneUp, 0, -4, 0).setType(matNine);
                        }
                    }
                }, duration);
            } else {
                Messaging.send(player, "No place!");
                return SkillResult.CANCELLED;
            }

        }

        broadcastExecuteText(hero);
        Messaging.send(player, "Duration: " + duration / 20 + "s");
        return SkillResult.NORMAL;
    }

    public static BlockFace getPlayerDirection(Player player) {
        Block wTargetBlock = player.getTargetBlock(
                (Set<Material>) null, MAX_DISTANCE);
        double wTargetX = wTargetBlock.getX();
        double wTargetZ = wTargetBlock.getZ();

        double wCenterX = player.getLocation().getX();
        double wCenterZ = player.getLocation().getZ();

        double wAngle = Math
                .atan((wTargetX - wCenterX) / (wCenterZ - wTargetZ))
                * (180 / Math.PI);

        if (wTargetX > wCenterX && wTargetZ > wCenterZ) {
            wAngle = (90 + wAngle) + 90;
        } else if (wTargetX < wCenterX && wTargetZ > wCenterZ) {
            wAngle = wAngle + 180;
        } else if (wTargetX < wCenterX && wTargetZ < wCenterZ) {
            wAngle = (90 + wAngle) + 270;
        }

        BlockFace wDirection = null;
        if (wAngle < 45) {
            // player facing east.
            wDirection = BlockFace.EAST;
        } else if (wAngle < 135) {
            // player facing south.
            wDirection = BlockFace.SOUTH;
        } else if (wAngle < 225) {
            // player facing west.
            wDirection = BlockFace.WEST;
        } else if (wAngle < 315) {
            // player facing north.
            wDirection = BlockFace.NORTH;
        } else if (wAngle < 360) {
            // player facing east.
            wDirection = BlockFace.EAST;
        }

        return wDirection;
    }

    public static int setRelativeBlocks(Player player, Block targetBlock, BlockFace blockFace, Material material, int num) {
        targetBlock = targetBlock.getRelative(blockFace);

        if (targetBlock.getType().equals(Material.AIR) && LocationUtil.isLocationPVP(targetBlock.getLocation())) {
            targetBlock.setType(material);
        } else return num + 1;

        if (num > 0) {
            num--;
            return setRelativeBlocks(player, targetBlock, blockFace, material, num);
        } else return 0;
    }

    private Block retrieveBlock(Block refBlock, int relX, int relY, int relZ) {
        Block returnBlock = refBlock;
        while (0 < Math.abs(relX)) {
            if (relX > 0) {
                returnBlock = returnBlock.getRelative(BlockFace.NORTH);
                relX--;
            } else {
                returnBlock = returnBlock.getRelative(BlockFace.SOUTH);
                relX++;
            }
        }
        while (0 < Math.abs(relY)) {
            if (relY > 0) {
                returnBlock = returnBlock.getRelative(BlockFace.EAST);
                relY--;
            } else {
                returnBlock = returnBlock.getRelative(BlockFace.WEST);
                relY++;
            }
        }
        while (0 < Math.abs(relZ)) {
            if (relZ > 0) {
                returnBlock = returnBlock.getRelative(BlockFace.UP);
                relZ--;
            } else {
                returnBlock = returnBlock.getRelative(BlockFace.DOWN);
                relZ++;
            }
        }
        return returnBlock;
    }


}