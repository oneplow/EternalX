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
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

/**
 * 13. Counter Strike: โอกาส X% ในการลดดาเมจครึ่งหนึ่ง และสวนทอนกลับ
 */
public class CounterStrikeAbility extends Ability {
  private final double chance; // 20.0 = 20%
  private final Random random;

  public CounterStrikeAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_DAMAGE_TAKEN, config);
    this.chance = getDoubleConfig(config, "chance", 20.0);
    this.random = new Random();
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (e.getEntity() != player) return false;
    
    // โอกาสติด
    if (random.nextDouble() * 100 > chance) return false;

    double originalDamage = e.getDamage();
    e.setDamage(originalDamage * 0.5); // บลดดาเมจ 50%

    // สวนกลับ
    if (e.getDamager() instanceof LivingEntity attacker) {
      attacker.damage(originalDamage, player);
      
      player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1);
      player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
    }
    
    return true;
  }
}
