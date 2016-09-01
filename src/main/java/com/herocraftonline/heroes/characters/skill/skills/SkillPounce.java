package com.herocraftonline.heroes.characters.skill.skills;


import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SkillPounce
  extends ActiveSkill
{
  private Set<Player> chargingPlayers = new HashSet();
  
  public SkillPounce(Heroes paramHeroes)
  {
    super(paramHeroes, "Pounce");
    setDescription("Jump up to $3 blocks to your target. AOE Radius:$2 Damage:$1");
    setUsage("/skill pounce");
    setArgumentRange(0, 1);
    setIdentifiers(new String[] { "skill pounce" });
    setTypes(new SkillType[] { SkillType.PHYSICAL, SkillType.MOVEMENT, SkillType.HARMFUL });
    Bukkit.getServer().getPluginManager().registerEvents(new ChargeEntityListener(this), this.plugin);
  }
  
  public String getDescription(Hero hero)
  {
    long stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 10000, false);
    if (stunDuration > 0L)
    {
      stunDuration = (long)(stunDuration + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
      stunDuration = stunDuration > 0L ? stunDuration : 0L;
    }
    long slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 0, false);
    if (slowDuration > 0L)
    {
      slowDuration = (long)(slowDuration + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
      slowDuration = slowDuration > 0L ? slowDuration : 0L;
    }
    long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 0, false);
    if (rootDuration > 0L)
    {
      rootDuration = (long)(rootDuration + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
      rootDuration = rootDuration > 0L ? rootDuration : 0L;
    }
    long silenceDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 0, false);
    if (silenceDuration > 0L)
    {
      silenceDuration = (long)(silenceDuration + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
      silenceDuration = silenceDuration > 0L ? silenceDuration : 0L;
    }
    long invulnDuration = SkillConfigManager.getUseSetting(hero, this, "invuln-duration", 0, false);
    if (invulnDuration > 0L)
    {
      invulnDuration = (long)(invulnDuration + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
      invulnDuration = invulnDuration > 0L ? invulnDuration : 0L;
    }
    int damage = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 0, false) + SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0.0D, false) * hero.getSkillLevel(this));
    
    damage = damage > 0 ? damage : 0;
    int radius = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 2, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE.node(), 0.0D, false) * hero.getSkillLevel(this));
    
    radius = radius > 0 ? radius : 0;
    int distance = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 15, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * hero.getSkillLevel(this));
    
    distance = distance > 0 ? distance : 0;
    String description = getDescription().replace("$1", damage + "").replace("$2", radius + "").replace("$3", distance + "");
    if (stunDuration > 0L) {
      description = description + " Stun:" + stunDuration + "s";
    }
    if (slowDuration > 0L) {
      description = description + " Slow:" + slowDuration + "s";
    }
    if (rootDuration > 0L) {
      description = description + " Root:" + rootDuration + "s";
    }
    if (silenceDuration > 0L) {
      description = description + " Silence:" + silenceDuration + "s";
    }
    if (invulnDuration > 0L) {
      description = description + " Invuln:" + invulnDuration + "s";
    }
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
  
  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection localConfigurationSection = super.getDefaultConfig();
    localConfigurationSection.set("stun-duration", Integer.valueOf(5000));
    localConfigurationSection.set("slow-duration", Integer.valueOf(0));
    localConfigurationSection.set("root-duration", Integer.valueOf(0));
    localConfigurationSection.set("silence-duration", Integer.valueOf(0));
    localConfigurationSection.set("invuln-duration", Integer.valueOf(0));
    localConfigurationSection.set("duration-increase", Integer.valueOf(0));
    localConfigurationSection.set(SkillSetting.DAMAGE.node(), Integer.valueOf(0));
    localConfigurationSection.set("damage-increase", Integer.valueOf(0));
    localConfigurationSection.set(SkillSetting.RADIUS.node(), Integer.valueOf(2));
    localConfigurationSection.set(SkillSetting.RADIUS_INCREASE.node(), Integer.valueOf(0));
    localConfigurationSection.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(15));
    localConfigurationSection.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), Integer.valueOf(0));
    localConfigurationSection.set(SkillSetting.USE_TEXT.node(), "%hero% used %skill%!");
    return localConfigurationSection;
  }
  
  public SkillResult use(Hero paramHero, String[] paramArrayOfString)
  {
    final Player localPlayer = paramHero.getPlayer();
    Location localLocation1 = localPlayer.getLocation();
    int distance = (int)(SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE.node(), 15, false) + SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * paramHero.getSkillLevel(this));
    
    distance = distance > 0 ? distance : 0;
    Location localLocation2 = localPlayer.getTargetBlock((Set<Material>)null, distance).getLocation();
    double d1 = localLocation2.getX() - localLocation1.getX();
    double d2 = localLocation2.getZ() - localLocation1.getZ();
    double d3 = Math.sqrt(d1 * d1 + d2 * d2);
    double d4 = localLocation2.distance(localLocation1) / 8.0D;
    d1 = d1 / d3 * d4;
    d2 = d2 / d3 * d4;
    localPlayer.setVelocity(new Vector(d1, 1.0D, d2));
    this.chargingPlayers.add(localPlayer);
    this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
    {
      public void run()
      {
        localPlayer.setFallDistance(8.0F);
      }
    }, 1L);
    
    broadcastExecuteText(paramHero);
    return SkillResult.NORMAL;
  }
  
  public class ChargeEntityListener
    implements Listener
  {
    private final Skill skill;
    
    public ChargeEntityListener(Skill arg2)
    {
      this.skill = arg2;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent paramEntityDamageEvent)
    {
      if ((!paramEntityDamageEvent.getCause().equals(EntityDamageEvent.DamageCause.FALL)) || (!(paramEntityDamageEvent.getEntity() instanceof Player)) || (!SkillPounce.this.chargingPlayers.contains((Player)paramEntityDamageEvent.getEntity())))
      {
        return;
      }
      Player localPlayer1 = (Player)paramEntityDamageEvent.getEntity();
      Hero localHero1 = SkillPounce.this.plugin.getCharacterManager().getHero(localPlayer1);
      SkillPounce.this.chargingPlayers.remove(localPlayer1);
      paramEntityDamageEvent.setDamage(0);
      paramEntityDamageEvent.setCancelled(true);
      int i = (int)(SkillConfigManager.getUseSetting(localHero1, this.skill, SkillSetting.RADIUS.node(), 2, false) + SkillConfigManager.getUseSetting(localHero1, this.skill, SkillSetting.RADIUS_INCREASE.node(), 0.0D, false) * localHero1.getSkillLevel(this.skill));
      
      i = i > 0 ? i : 0;
      long l1 = SkillConfigManager.getUseSetting(localHero1, this.skill, "stun-duration", 10000, false);
      if (l1 > 0L)
      {
        l1 = (long)(l1 + SkillConfigManager.getUseSetting(localHero1, this.skill, "duration-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
        l1 = l1 > 0L ? l1 : 0L;
      }
      long l2 = SkillConfigManager.getUseSetting(localHero1, this.skill, "slow-duration", 0, false);
      if (l2 > 0L)
      {
        l2 = (long)(l2 + SkillConfigManager.getUseSetting(localHero1, this.skill, "duration-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
        l2 = l2 > 0L ? l2 : 0L;
      }
      long l3 = SkillConfigManager.getUseSetting(localHero1, this.skill, "root-duration", 0, false);
      if (l3 > 0L)
      {
        l3 = (long)(l3 + SkillConfigManager.getUseSetting(localHero1, this.skill, "duration-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
        l3 = l3 > 0L ? l3 : 0L;
      }
      long l4 = SkillConfigManager.getUseSetting(localHero1, this.skill, "silence-duration", 0, false);
      if (l4 > 0L)
      {
        l4 = (long)(l4 + SkillConfigManager.getUseSetting(localHero1, this.skill, "duration-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
        l4 = l4 > 0L ? l4 : 0L;
      }
      int j = (int)(SkillConfigManager.getUseSetting(localHero1, this.skill, SkillSetting.DAMAGE.node(), 0, false) + SkillConfigManager.getUseSetting(localHero1, this.skill, "damage-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
      
      j = j > 0 ? j : 0;
      long l5 = SkillConfigManager.getUseSetting(localHero1, this.skill, "invuln-duration", 0, false);
      if (l5 > 0L)
      {
        l5 = (long)(l5 + SkillConfigManager.getUseSetting(localHero1, this.skill, "duration-increase", 0.0D, false) * localHero1.getSkillLevel(this.skill));
        l5 = l5 > 0L ? l5 : 0L;
        if (l5 > 0L) {
          localHero1.addEffect(new InvulnerabilityEffect(this.skill, l5));
        }
      }
      Iterator localIterator = localPlayer1.getNearbyEntities(i, i, i).iterator();
      while (localIterator.hasNext())
      {
        Entity localEntity = (Entity)localIterator.next();
        if ((localEntity instanceof LivingEntity))
        {
          LivingEntity localLivingEntity = (LivingEntity)localEntity;
          if (Skill.damageCheck(localPlayer1, localLivingEntity))
          {
            if ((localEntity instanceof Player))
            {
              Player localPlayer2 = (Player)localEntity;
              Hero localHero2 = SkillPounce.this.plugin.getCharacterManager().getHero(localPlayer2);
              if (l1 > 0L) {
                localHero2.addEffect(new StunEffect(this.skill, l1));
              }
              if (l2 > 0L) {
                localHero2.addEffect(new SlowEffect(this.skill, l2, 2, true, localPlayer2.getDisplayName() + " has been slowed by " + localPlayer1.getDisplayName(), localPlayer2.getDisplayName() + " is no longer slowed by " + localPlayer1.getDisplayName(), localHero1));
              }
              if (l3 > 0L) {
                localHero2.addEffect(new RootEffect(this.skill, l3));
              }
              if (l4 > 0L) {
                localHero2.addEffect(new SilenceEffect(this.skill, l4));
              }
              if (j > 0) {
                Skill.damageEntity(localLivingEntity, localPlayer1, j, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
              }
            }
            if (j > 0) {
              Skill.damageEntity(localLivingEntity, localPlayer1, j, EntityDamageEvent.DamageCause.MAGIC);
            }
          }
        }
      }
    }
  }
}
