package com.warakorn.eternalx.modules.items.abilities.defense;

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
 * 12. Absorption: Passive ทุกๆ x วินาที จะให้เกราะหัวใจสีทอง 
 */
public class AbsorptionAbility extends Ability {
  private final int amplifier;

  public AbsorptionAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.PASSIVE, config);
    this.amplifier = getIntConfig(config, "amplifier", 0); // 0 = 2 hearts
    
    // ตั้ง default แบบ passive (PASSIVE ทำงานทุกวินาที เราไม่อยากให้ปั๊มเรื่อยๆ ต้องมี cooldown)
    // Cooldown สำหรับ Passive เราจะตั้งให้เช็คพอดีกับระยะเวลา Effect
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) return false;

    // เราเช็คว่าถ้ายังไม่ติด Cooldown ถึงจะให้ Effect ไป 
    // แล้ว set Cooldown ของ Ability ยาวๆ (เช่น 30 วิ)
    // ตรงนี้นอกจากเพิ่ม cooldown, เราจะให้ Heart กลับมาเต็ม
    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, amplifier, false, false, true)); // 30s
    return true; 
    // ถ้ารีเทิร์น true จะกิน Cooldown ทันทีตามที่ตั้งใน config (เช่น 30 วินาที = 600 ticks)
  }
}
