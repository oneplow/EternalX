package com.warakorn.eternalx.modules.items.abilities.defense;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 9. Resurrect: ถ้าโดนตีตาย จะฟื้นตัวพร้อมพลังชีวิตที่กำหนด (เปรียบเหมือน Totem)
 */
public class ResurrectAbility extends Ability {
  private final double healPercent; // 100 = เต็มหลอด

  public ResurrectAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_DAMAGE_TAKEN, config);
    this.healPercent = getDoubleConfig(config, "heal_percent", 100.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageEvent e)) return false;

    if (player.getHealth() - e.getFinalDamage() <= 0) {
      e.setCancelled(true); // ยกเลิกการตาย

      double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
      double healAmount = maxHealth * (healPercent / 100.0);
      
      player.setHealth(Math.min(maxHealth, healAmount));
      player.setFireTicks(0);
      player.setFallDistance(0);
      
      // Totem effect
      player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.5);
      player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
      
      return true; // สำเร็จ -> ติด cooldown
    }

    return false;
  }
}
