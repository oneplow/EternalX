package com.warakorn.eternalx.modules.items.abilities.defense;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 10. Damage Reflect: สะท้อนดาเมจ X% กลับไปหาคนตี (เป็นแบบ PASSIVE / ON_DAMAGE_TAKEN โดยไม่ต้องกด)
 */
public class DamageReflectAbility extends Ability {
  private final double reflectPercent; // 50.0 = 50% สะท้อนกลับ

  public DamageReflectAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_DAMAGE_TAKEN, config);
    this.reflectPercent = getDoubleConfig(config, "percent", 50.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    
    // ผู้เล่นเป็นคนถูกตี 
    if (e.getEntity() != player) return false;
    
    // คนตีคือสิ่งมีชีวิตอื่น
    if (!(e.getDamager() instanceof LivingEntity attacker)) return false;

    double originalDamage = e.getDamage();
    double reflectAmount = originalDamage * (reflectPercent / 100.0);

    if (reflectAmount > 0) {
      attacker.damage(reflectAmount, player);
      
      // Effect สะท้อนดาเมจ เป็นสะเก็ดไฟเบาๆ
      player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
      player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 2.0f);
      return true;
    }

    return false;
  }
}
