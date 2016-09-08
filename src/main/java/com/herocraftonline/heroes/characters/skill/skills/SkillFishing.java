package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

public class SkillFishing
  extends PassiveSkill
{
  public SkillFishing(Heroes plugin)
  {
    super(plugin, "Fishing");
    setDescription("You have a $1% chance of getting a bonus fish!");
    setEffectTypes(EffectType.BENEFICIAL);
    setTypes(SkillType.KNOWLEDGE, SkillType.EARTH, SkillType.BUFF);
    Bukkit.getServer().getPluginManager().registerEvents(new SkillPlayerListener(this), plugin);
  }
  
  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set("chance-per-level", Double.valueOf(0.001D));
    node.set("leather-level", Integer.valueOf(5));
    node.set("enable-leather", Boolean.valueOf(false));
    return node;
  }
  
  public class SkillPlayerListener
    implements Listener
  {
    private final Skill skill;
    
    SkillPlayerListener(Skill skill)
    {
      this.skill = skill;
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event)
    {
      if ((event.isCancelled()) || (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) || (!(event.getCaught() instanceof Item))) {
        return;
      }
      Item getCaught = (Item)event.getCaught();
      double chance = Util.nextRand();
      Hero hero = SkillFishing.this.plugin.getCharacterManager().getHero(event.getPlayer());
      Player player = hero.getPlayer();
      if (chance < SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.CHANCE_LEVEL, 0.001D, false) * hero.getSkillLevel(this.skill))
      {
        int leatherlvl = SkillConfigManager.getUseSetting(hero, this.skill, "leather-level", 5, true);
        if ((hero.getLevel() >= leatherlvl) && (SkillConfigManager.getUseSetting(hero, this.skill, "enable-leather", false)))
        {
          if (getCaught != null) {
            switch (Util.nextInt(6))
            {
            case 0: 
              getCaught.setItemStack(new ItemStack(Material.LEATHER_BOOTS, 1));
              Messaging.send(player, "You found leather boots!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 40.0D));
              break;
            case 1: 
              getCaught.setItemStack(new ItemStack(Material.LEATHER_LEGGINGS, 1));
              Messaging.send(player, "You found leather leggings!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 46.0D));
              break;
            case 2: 
              getCaught.setItemStack(new ItemStack(Material.LEATHER_HELMET, 1));
              Messaging.send(player, "You found a leather helmet!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 34.0D));
              break;
            case 3: 
              getCaught.setItemStack(new ItemStack(Material.LEATHER_CHESTPLATE, 1));
              Messaging.send(player, "You found a leather chestplate!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 49.0D));
              break;
            case 4: 
              getCaught.setItemStack(new ItemStack(Material.GOLDEN_APPLE, 1));
              Messaging.send(player, "You found a golden apple, woo!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 10.0D));
              break;
            case 5: 
              getCaught.setItemStack(new ItemStack(Material.APPLE, 1));
              Messaging.send(player, "You found an apple!");
              getCaught.getItemStack().setDurability((short)(int)(Math.random() * 29.0D));
              break;
            case 6: 
              getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
              Messaging.send(player, "You found 2 Fishes!");
              break;
            case 7: 
              getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
              Messaging.send(player, "You found 1 Fish!");
            }
          }
        }
        else {
          switch (Util.nextInt(2))
          {
          case 0: 
            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 2));
            Messaging.send(player, "You found 2 Fishes!");
            break;
          case 1: 
            getCaught.setItemStack(new ItemStack(Material.RAW_FISH, 1));
            Messaging.send(player, "You found 1 Fish!");
          }
        }
      }
    }
  }
  
  public String getDescription(Hero hero)
  {
    double chance = SkillConfigManager.getUseSetting(hero, this, "chance-per-level", 0.001D, false);
    int level = hero.getSkillLevel(this);
    if (level < 1) {
      level = 1;
    }
    return getDescription().replace("$1", Util.stringDouble(chance * level * 100.0D));
  }
}
