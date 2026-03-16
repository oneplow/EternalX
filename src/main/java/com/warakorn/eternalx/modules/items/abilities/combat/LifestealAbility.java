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
 * 1. Lifesteal: ดูด HP ศัตรู % คืนตัวเองเมื่อตี
 */
public class LifestealAbility extends Ability {
  private final double percent; // 10 = 10%
  
  public LifestealAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.percent = getDoubleConfig(config, "heal_percent", 10.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity)) return false;

    double damage = e.getFinalDamage();
    double healAmount = damage * (percent / 100.0);
    
    if (healAmount > 0) {
      double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
      double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);
      player.setHealth(newHealth);
      
      // Spawn particle ขโมยเลือด (แดงๆ)
      // plugin.getServer().getScheduler().runTask(...) // ถือว่าละไว้เพื่อความกระชับ
      return true;
    }
    
    return false;
  }
}
