package com.warakorn.eternalx.modules.items.abilities.movement;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 17. Double Jump: ตรวจจับการกดบิน (กระโดดกลางอากาศ 2 ครั้งติด)
 * หมายเหตุ: ต้องใช้ Listener พิเศษเพิ่มเติม เพราะ AbilityType ปกติไม่ครอบคลุม 
 * แต่เราสามารถผูกกับการกดแอบ (Sneak) แทน หรือใช้ Passive ให้ AllowFlight = true ไว้ก็อปปี้ flight event
 * เพื่อความชัวร์และง่ายกับคนเล่น Bedrock เราใช้ PASSIVE trigger ให้ AllowFlight แล้วจับ EVENT ต่างหาก
 */
public class DoubleJumpAbility extends Ability {
  private final double forceXZ; // ความไกลแนวนอน 1.2
  private final double forceY;  // ความสูงแนวตั้ง 0.8

  public DoubleJumpAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.PASSIVE, config);
    this.forceXZ = getDoubleConfig(config, "force_xz", 1.2);
    this.forceY = getDoubleConfig(config, "force_y", 0.8);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    // 1. กระบวนการ PASSIVE: ทำหน้าที่แค่แจก AllowFlight ให้ผู้เล่นเพื่อเตรียม Double Jump
    if (event == null && player.getGameMode() == GameMode.SURVIVAL && !player.getAllowFlight()) {
      player.setAllowFlight(true);
      return false; // ยังไม่ได้กระโดด แค่เตรียมพร้อม
    }

    // 2. กระบวนการจับ Event พิเศษ: รับรู้การกดบาร์โหลการบิน (Fly)
    if (event instanceof PlayerToggleFlightEvent e && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
      e.setCancelled(true); // ยกเลิกการบิน
      player.setAllowFlight(false); // กันบินต่อ
      player.setFlying(false);

      // เด้ง
      Vector jump = player.getLocation().getDirection().multiply(forceXZ).setY(forceY);
      player.setVelocity(jump);

      player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation(), 15, 0.5, 0.1, 0.5, 0.05);
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
      
      return true; // สำเร็จ กิน Cooldown
    }

    return false;
  }
}
