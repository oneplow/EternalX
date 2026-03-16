package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 4. Execute: โจมตีแรงขึ้น X เท่า หากศพมี HP ต่ำกว่า % ที่กำหนดแปล
 */
public class ExecuteAbility extends Ability {
  private final double threshold; // 30.0 = ศัตรูเลือดต่ำกว่า 30%
  private final double multiplier; // 2.0 = โดนดาเมจแรง 2 เท่า

  public ExecuteAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.threshold = getDoubleConfig(config, "threshold", 30.0);
    this.multiplier = getDoubleConfig(config, "multiplier", 2.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    double maxHp = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
    double currentHp = victim.getHealth();
    double percentHp = (currentHp / maxHp) * 100.0;

    if (percentHp <= threshold) {
      double originalDamage = e.getDamage();
      e.setDamage(originalDamage * multiplier);

      // Effect ประหาร
      victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0, 1, 0), 15);
      victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
      return true;
    }

    return false;
  }
}
