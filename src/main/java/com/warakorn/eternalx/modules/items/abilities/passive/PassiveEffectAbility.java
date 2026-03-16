package com.warakorn.eternalx.modules.items.abilities.passive;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * 28-30 Passive Effects (ใช้ระบบเดียวกัน)
 * โดนรันทุก 1s (20 ticks) จาก AbilityListener ท่า PASSIVE
 */
public class PassiveEffectAbility extends Ability {
  private final PotionEffectType effectType;
  private final int amplifier;

  public PassiveEffectAbility(EternalX plugin, String id, Map<String, Object> config, PotionEffectType effectType) {
    super(plugin, id, AbilityType.PASSIVE, config);
    this.effectType = effectType;
    this.amplifier = getIntConfig(config, "amplifier", 0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    // ให้ effect นาน 2 วินาที (40 ticks) เพื่อไม่ให้หายไปกระพริบกลางคัน 
    // เพราะ passive tick ยิงทุก 1s
    player.addPotionEffect(new PotionEffect(effectType, 40, amplifier, false, false, true));
    return false; // ไม่ชนกับ Cooldown
  }
}
