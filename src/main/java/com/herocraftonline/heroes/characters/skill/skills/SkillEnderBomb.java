package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import me.kapehh.main.pluginmanager.utils.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;

public class SkillEnderBomb extends ActiveSkill {
    private final Map<EnderPearl, Hero> enderbombMap = new WeakHashMap<>();
    private final List<String> enderbombThrowers = new ArrayList<>();

    public SkillEnderBomb(Heroes plugin) {
        super(plugin, "EnderBomb");
        setDescription("Throw enderbomb, which deals $1 + explosion damage. Explosion radius: $2.");
        setUsage("/skill enderbomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill enderbomb");
        setTypes(SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FIRE);

        Bukkit.getPluginManager().registerEvents(new SkillEnderBombListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.REAGENT.node(), Material.ENDER_PEARL.getId());
        node.set(SkillSetting.REAGENT_COST.node(), 1);
        node.set(SkillSetting.DAMAGE.node(), 15.0D);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.3D);
        node.set("explosion-radius", 3.0D);
        node.set("fire", false);
        node.set("prevent-wilderness-block-damage-and-fire", true);
        node.set("damage-friendly", false);
        return node;
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getDamage(hero))).replace("$2", Util.stringDouble(getExplosionRadius(hero))));
        if (!isDamageFriendly(hero)) {
            descr.append(" No friendly damage.");
        }
        int cd = getCooldown(hero);
        if (cd > 0) {
            descr.append(" CD:");
            descr.append(Util.stringDouble(cd / 1000.0D));
            descr.append("s");
        }
        int mana = getMana(hero);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }
        return descr.toString();
    }

    public int getDamage(Hero hero) {
        return (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15.0D, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.3D, false) * hero.getSkillLevel(this));
    }

    public float getExplosionRadius(Hero hero) {
        return (float) SkillConfigManager.getUseSetting(hero, this, "explosion-radius", 3.0D, false);
    }

    public boolean isFire(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "fire", false);
    }

    public boolean isNoGrief(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "prevent-wilderness-block-damage-and-fire", true);
    }

    public boolean isDamageFriendly(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "damage-friendly", false);
    }

    public int getCooldown(Hero hero) {
        return Math.max(0, SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, true) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getSkillLevel(this));
    }

    public int getMana(Hero hero) {
        return (int) Math.max(0.0D, SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0.0D, true) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0.0D, false) * hero.getSkillLevel(this));
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        EnderPearl enderBomb = player.launchProjectile(EnderPearl.class);
        this.enderbombMap.put(enderBomb, hero);
        this.enderbombThrowers.add(player.getName());

        return SkillResult.NORMAL;
    }

    public class SkillEnderBombListener implements Listener {
        private long currentTick = 0L;
        private Location currentLandingLoc;
        private Hero currentThrower;

        public SkillEnderBombListener() {
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if ((event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) &&
                    (SkillEnderBomb.this.enderbombThrowers.contains(event.getPlayer().getName()))) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
            if ((event.getRemover() instanceof Player)) {
                Player remover = (Player) event.getRemover();
                if (SkillEnderBomb.this.enderbombThrowers.contains(remover.getName())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onProjectileHit(ProjectileHitEvent event) {
            if (event.getEntityType() == EntityType.ENDER_PEARL) {
                EnderPearl enderPearl = (EnderPearl) event.getEntity();
                Hero thrower = SkillEnderBomb.this.enderbombMap.remove(enderPearl);
                if (thrower != null) {
                    if (thrower.getPlayer().isOnline()) {
                        this.currentThrower = thrower;
                        this.currentTick = getFirstWorldTime();
                        this.currentLandingLoc = enderPearl.getLocation();

                        // Если запрещено пвп, то не даем взрываться
                        if (!LocationUtil.isLocationPVP(this.currentLandingLoc)) {
                            Messaging.send(this.currentThrower.getPlayer(), "This territory is NoPVP!");
                        } else {
                            enderPearl.getWorld().createExplosion(this.currentLandingLoc, SkillEnderBomb.this.getExplosionRadius(thrower), SkillEnderBomb.this.isFire(thrower));
                        }
                    }
                    SkillEnderBomb.this.enderbombThrowers.remove(thrower.getPlayer().getName());
                    enderPearl.remove();
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityExplode(EntityExplodeEvent event) {
            if ((event.getEntity() == null) && (getFirstWorldTime() == this.currentTick) && (this.currentLandingLoc.distanceSquared(event.getLocation()) < 4.0D)) {
                if ((SkillEnderBomb.this.isNoGrief(this.currentThrower)) || (event.isCancelled())) {
                    event.blockList().clear();
                    event.setYield(0.0F);
                }
                event.setCancelled(false);
            }
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                return;
            }
            if (getFirstWorldTime() != this.currentTick) {
                return;
            }
            if (!(event.getEntity() instanceof LivingEntity)) {
                event.setCancelled(true);
                return;
            }
            LivingEntity target = (LivingEntity) event.getEntity();
            if (this.currentLandingLoc.distanceSquared(target.getLocation()) > 625.0D) {
                return;
            }
            if ((SkillEnderBomb.this.isDamageFriendly(this.currentThrower)) || (!isFriendlyTo(target, this.currentThrower))) {
                Skill.damageEntity(target, this.currentThrower.getPlayer(), SkillEnderBomb.this.getDamage(this.currentThrower), EntityDamageEvent.DamageCause.MAGIC);
            } else {
                event.setCancelled(true);
            }
        }

        private long getFirstWorldTime() {
            return Bukkit.getWorlds().get(0).getFullTime();
        }

        private boolean isFriendlyTo(LivingEntity target, Hero hero) {
            Player player = hero.getPlayer();
            if (target.equals(player)) {
                return true;
            }
            Set<String> friendlyNames = new HashSet();
            friendlyNames.add(player.getName());

            HeroParty party = hero.getParty();
            if (party != null) {
                if (((target instanceof Player)) && (party.isPartyMember((Player) target))) {
                    return true;
                }
                for (Hero partyMember : party.getMembers()) {
                    friendlyNames.add(partyMember.getName());
                }
            }
            if ((target instanceof Tameable)) {
                Tameable tameable = (Tameable) target;
                if ((tameable.isTamed()) && (friendlyNames.contains(tameable.getOwner().getName()))) {
                    return true;
                }
            }
            return false;
        }
    }
}
