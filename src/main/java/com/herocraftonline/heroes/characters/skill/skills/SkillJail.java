package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;

public class SkillJail
  extends TargettedSkill
{
  private String jailText;
  private Map<Player, Location> jailedPlayers = new HashMap();
  private Set<Location> jailLocations;
  
  public SkillJail(Heroes plugin)
  {
    super(plugin, "Jail");
    setDescription("Taunt a player for $1s. If that player dies while taunted, they go to Jail. R:$2");
    setUsage("/skill jail");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill jail" });
    
    setTypes(new SkillType[] { SkillType.HARMFUL, SkillType.TELEPORT });
    Bukkit.getServer().getPluginManager().registerEvents(new RespawnListener(), plugin);
    Bukkit.getServer().getPluginManager().registerEvents(new JailListener(), plugin);
  }
  
  public String getDescription(Hero hero)
  {
    long duration = (long) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 60000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
    
    duration = duration > 0L ? duration : 0L;
    int distance = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 15, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * hero.getSkillLevel(this));
    
    distance = distance > 0 ? distance : 0;
    String description = getDescription().replace("$1", duration + "").replace("$2", distance + "");
    
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
    ConfigurationSection node = super.getDefaultConfig();
    String worldName = ((World)this.plugin.getServer().getWorlds().get(0)).getName();
    node.set("jail-text", "%target% was jailed!");
    node.set("default", worldName + ":0:65:0");
    node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(25));
    node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), Integer.valueOf(0));
    node.set(SkillSetting.DURATION.node(), Integer.valueOf(60000));
    node.set("duration-increase", Integer.valueOf(0));
    return node;
  }
  
  public void init()
  {
    super.init();
    this.jailLocations = new HashSet();
    this.jailText = SkillConfigManager.getRaw(this, "jail-text", "%target% was jailed!").replace("%target%", "$1");
    for (String n : SkillConfigManager.getRawKeys(this, null))
    {
      String retrievedNode = SkillConfigManager.getRaw(this, n, (String)null);
      if (retrievedNode != null)
      {
        String[] splitArg = retrievedNode.split(":");
        if ((retrievedNode != null) && (splitArg.length == 4))
        {
          World world = this.plugin.getServer().getWorld(splitArg[0]);
          this.jailLocations.add(new Location(world, Double.parseDouble(splitArg[1]), Double.parseDouble(splitArg[2]), Double.parseDouble(splitArg[3])));
        }
      }
    }
  }
  
  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    if (this.jailLocations.isEmpty())
    {
      Messaging.send(hero.getPlayer(), "There are no jails setup yet.", new Object[0]);
      return SkillResult.INVALID_TARGET_NO_MSG;
    }
    Player player = hero.getPlayer();
    if (target.equals(player)) {
      return SkillResult.INVALID_TARGET;
    }
    if (((target instanceof Player)) && (damageCheck((Player)target, player)))
    {
      Hero tHero = this.plugin.getCharacterManager().getHero((Player)target);
      long duration = (long) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 60000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this));
      
      duration = duration > 0L ? duration : 0L;
      tHero.addEffect(new JailEffect(this, duration));
      broadcastExecuteText(hero, target);
    }
    return SkillResult.NORMAL;
  }
  
  public class JailEffect
    extends ExpirableEffect
  {
    public JailEffect(Skill skill, long duration)
    {
      super(skill, "Jail", duration);
    }
  }
  
  public class JailListener
    implements Listener
  {
    public JailListener() {}
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
      if ((event.isCancelled()) || (!(event.getEntity() instanceof Player)) || (event.getDamage() == 0) || (SkillJail.this.jailLocations.isEmpty()) || (event.getDamage() < SkillJail.this.plugin.getCharacterManager().getHero((Player)event.getEntity()).getPlayer().getHealth())) {
        return;
      }
      Player player = (Player)event.getEntity();
      Hero hero = SkillJail.this.plugin.getCharacterManager().getHero(player);
      if (hero.hasEffect("Jail"))
      {
        SkillJail.this.broadcast(player.getLocation(), SkillJail.this.jailText, new Object[] { player.getDisplayName() });
        Location tempLocation = null;
        for (Location l : SkillJail.this.jailLocations) {
          if ((tempLocation == null) || (tempLocation.distanceSquared(player.getLocation()) > l.distanceSquared(player.getLocation()))) {
            tempLocation = l;
          }
        }
        SkillJail.this.jailedPlayers.put(player, tempLocation);
      }
    }
  }
  
  public class RespawnListener
    implements Listener
  {
    public RespawnListener() {}
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
      if ((!SkillJail.this.jailedPlayers.isEmpty()) && (SkillJail.this.jailedPlayers.containsKey(event.getPlayer())))
      {
        event.setRespawnLocation((Location)SkillJail.this.jailedPlayers.get(event.getPlayer()));
        SkillJail.this.jailedPlayers.remove(event.getPlayer());
      }
    }
  }
}
