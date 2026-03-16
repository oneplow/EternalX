package com.warakorn.eternalx.modules.items.abilities.utility;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 20. Heal Aura: ร่ายฮีลตัวเองและผู้เล่นรอบข้าง
 */
public class HealAuraAbility extends Ability {
  private final double amount; // Heal 5.0 (2.5 hearts)
  private final double radius;

  public HealAuraAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.amount = getDoubleConfig(config, "amount", 5.0);
    this.radius = getDoubleConfig(config, "radius", 8.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    // ฮีลตัวเอง
    heal(player);

    // ฮีลเพื่อน
    for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
      if (e instanceof Player target) {
        heal(target);
      }
    }

    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 20, 2, 0.5, 2, 0);
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);

    return true;
  }

  private void heal(Player p) {
    double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
    p.setHealth(Math.min(max, p.getHealth() + amount));
    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
  }
}
