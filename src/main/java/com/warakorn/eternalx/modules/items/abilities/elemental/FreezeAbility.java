package com.warakorn.eternalx.modules.items.abilities.elemental;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * 25. Freeze: สโลว์ + ตีช้าลง + particle น้ำแข็ง
 */
public class FreezeAbility extends Ability {
  private final int durationTicks;
  private final int amplifier;

  public FreezeAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.durationTicks = getIntConfig(config, "duration", 60); // 3 วินาที
    this.amplifier = getIntConfig(config, "amplifier", 1); // Slowness II
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier));
    victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, amplifier)); // ตีช้าลงด้วย

    victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
    return true;
  }
}
