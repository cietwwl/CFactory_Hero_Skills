package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SkillAirStrike extends ActiveSkill implements Listener {
    private HashMap<TNTPrimed, Player> explosions = new HashMap<TNTPrimed, Player>();
    
    public SkillAirStrike(Heroes plugin) {
        super(plugin, "AirStrike");
        setDescription("Drops armed TNT in a line at target block.");
        setUsage("/skill airstrike");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill airstrike" });
        Bukkit.getPluginManager().registerEvents(this, plugin);
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL);
    }

    @Override
    public String getDescription(Hero hero) {
        String description = getDescription();
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
        return node;
    }
    
    @Override
    public void init() {
        super.init();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location pLoc = player.getLocation();
        int maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 50, false);
        Block block = player.getTargetBlock((Set<Material>)null, maxDistance);
        Location bLoc = block.getLocation();
        World world = block.getWorld();
        double xDiff = bLoc.getX() - pLoc.getX();
        double zDiff = bLoc.getZ() - pLoc.getZ();
        double distance = Math.sqrt(Math.abs(Math.pow(xDiff, 2) + Math.pow(zDiff, 2)));
        double modifier = 10 / distance;
        xDiff *= modifier;
        zDiff *= modifier;
        
        for (int i = 0; i < 4; i++) {
            double newX = bLoc.getX() + (xDiff * i);
            double newZ = bLoc.getZ() + (zDiff * i);
            Location groundLevel = world.getHighestBlockAt(new Location(world, newX, 1, newZ)).getLocation();
            double newY = groundLevel.getY() + 20;
            newY = newY > world.getMaxHeight() ? world.getMaxHeight() : newY;
            TNTPrimed tnt = world.spawn(new Location(world, newX, newY, newZ), TNTPrimed.class);
            tnt.setFuseTicks(80 + (i * 10));
            System.out.println(newX + ":" + newY + ":" + newZ);
            explosions.put(tnt, player);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @EventHandler
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (explosions.containsKey(event.getEntity())) {
            event.getEntity().getWorld().createExplosion(event.getLocation(), 0.0F, false);
            for (Entity e : event.getEntity().getNearbyEntities(6, 6, 6)) {
                try {
                    LivingEntity le = (LivingEntity) e;
                    Hero hero = plugin.getCharacterManager().getHero((Player) explosions.get(event.getEntity()));
                    addSpellTarget(le,hero);
                    damageEntity(le, hero.getPlayer(), SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10D, false));
                } catch (Exception ex) {
                }
            }
            explosions.remove(event.getEntity());
            event.setCancelled(true);
        }
    }
}