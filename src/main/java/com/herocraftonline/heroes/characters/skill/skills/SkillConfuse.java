package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import java.util.Random;
import java.util.Set;
import net.minecraft.server.v1_8_R1.MathHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillConfuse
  extends TargettedSkill
{
  private static final Random random = new Random();
  private String applyText;
  private String expireText;
  
  public SkillConfuse(Heroes plugin)
  {
    super(plugin, "Confuse");
    setDescription("You confuse the target for $1 seconds.");
    setUsage("/skill confuse <target>");
    setArgumentRange(0, 1);
    setIdentifiers("skill confuse");
    setTypes(SkillType.SILENCABLE, SkillType.ILLUSION, SkillType.HARMFUL);
  }
  
  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
    node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1000));
    node.set("max-drift", Double.valueOf(0.35D));
    node.set(SkillSetting.APPLY_TEXT.node(), "%target% is confused!");
    node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!");
    return node;
  }
  
  public void init()
  {
    super.init();
    this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is confused!").replace("%target%", "$1");
    this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has regained his wit!").replace("%target%", "$1");
  }
  
  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
    long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, true);
    float maxDrift = (float)SkillConfigManager.getUseSetting(hero, this, "max-drift", 0.35D, false);
    this.plugin.getCharacterManager().getCharacter(target).addEffect(new ConfuseEffect(this, duration, period, maxDrift));
    broadcastExecuteText(hero, target);
    return SkillResult.NORMAL;
  }
  
  public class ConfuseEffect
    extends PeriodicExpirableEffect
  {
    private final float maxDrift;
    
    public ConfuseEffect(Skill skill, long duration, long period, float maxDrift)
    {
      super(skill, "Confuse", period, duration);
      this.maxDrift = maxDrift;
      this.types.add(EffectType.HARMFUL);
      this.types.add(EffectType.DISPELLABLE);
      this.types.add(EffectType.MAGIC);
      addMobEffect(9, (int)(duration / 1000L) * 20, 127, false);
    }
    
    public void adjustVelocity(LivingEntity lEntity)
    {
      Vector velocity = lEntity.getVelocity();
      
      float angle = SkillConfuse.random.nextFloat() * 2.0F * 3.14159F;
      float xAdjustment = this.maxDrift * MathHelper.cos(angle);
      float zAdjustment = this.maxDrift * MathHelper.sin(angle);
      
      velocity.add(new Vector(xAdjustment, 0.0F, zAdjustment));
      velocity.setY(0);
      lEntity.setVelocity(velocity);
    }
    
    public void applyToMonster(Monster monster)
    {
      super.applyToMonster(monster);
    }
    
    public void applyToHero(Hero hero)
    {
      super.applyToHero(hero);
      Player player = hero.getPlayer();
      broadcast(player.getLocation(), SkillConfuse.this.applyText, player.getDisplayName());
    }
    
    public void removeFromMonster(Monster monster)
    {
      super.removeFromMonster(monster);
      broadcast(monster.getEntity().getLocation(), SkillConfuse.this.expireText, Messaging.getLivingEntityName(monster));
    }
    
    public void removeFromHero(Hero hero)
    {
      super.removeFromHero(hero);
      Player player = hero.getPlayer();
      broadcast(player.getLocation(), SkillConfuse.this.expireText, player.getDisplayName());
    }
    
    public void tickMonster(Monster monster)
    {
      adjustVelocity(monster.getEntity());
      if ((monster instanceof Creature)) {
        ((Creature)monster).setTarget(null);
      }
    }
    
    public void tickHero(Hero hero)
    {
      adjustVelocity(hero.getPlayer());
    }
  }
  
  public String getDescription(Hero hero)
  {
    int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
    return getDescription().replace("$1", duration / 1000 + "");
  }
}
