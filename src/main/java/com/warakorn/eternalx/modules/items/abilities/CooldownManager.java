package com.warakorn.eternalx.modules.items.abilities;

import com.warakorn.eternalx.EternalX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * จัดการเรื่องคูลดาวน์สำหรับ Ability แต่ละชนิดแบบแยกรายคน
 */
public class CooldownManager {
  private final EternalX plugin;
  
  // Map<PlayerUUID, Map<AbilityID, NextAvailableTimeMillis>>
  private final Map<UUID, Map<String, Long>> cooldowns;

  public CooldownManager(EternalX plugin) {
    this.plugin = plugin;
    this.cooldowns = new HashMap<>();
  }

  /**
   * เช็คว่าผู้เล่นติด Cooldown ของ Ability นั้นไหม
   */
  public boolean isOnCooldown(Player player, String abilityId) {
    Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
    if (playerCooldowns == null) return false;

    Long nextAvailable = playerCooldowns.get(abilityId);
    if (nextAvailable == null) return false;

    return System.currentTimeMillis() < nextAvailable;
  }

  /**
   * Set Cooldown ให้ผู้เล่น เริ่มนับถอยหลัง
   */
  public void setCooldown(Player player, String abilityId, long ticks) {
    if (ticks <= 0) return;

    long millis = ticks * 50L; // 1 tick = 50ms
    long nextAvailable = System.currentTimeMillis() + millis;

    cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
        .put(abilityId, nextAvailable);
  }

  /**
   * ดึงเวลาที่เหลือ (หน่วยเป็นวินาที ทศนิยม 1 ตำแหน่ง)
   */
  public double getRemainingSeconds(Player player, String abilityId) {
    Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
    if (playerCooldowns == null) return 0.0;

    Long nextAvailable = playerCooldowns.get(abilityId);
    if (nextAvailable == null) return 0.0;

    long remainingMillis = nextAvailable - System.currentTimeMillis();
    if (remainingMillis <= 0) return 0.0;

    return remainingMillis / 1000.0;
  }

  /**
   * ส่งข้อความ Action Bar เตือนว่าติด cooldown
   */
  public void sendCooldownMessage(Player player, String abilityId) {
    double remaining = getRemainingSeconds(player, abilityId);
    if (remaining > 0) {
      player.sendActionBar(Component.text(String.format("⏳ Ability on cooldown! (%.1fs)", remaining), NamedTextColor.RED));
    }
  }

  /**
   * ล้าง cooldown ทั้งหมดของผู้เล่น
   */
  public void clearCooldowns(Player player) {
    cooldowns.remove(player.getUniqueId());
  }

  /**
   * ล้างของทุกคน (ใช้ตอน Reload)
   */
  public void clearAll() {
    cooldowns.clear();
  }
}
