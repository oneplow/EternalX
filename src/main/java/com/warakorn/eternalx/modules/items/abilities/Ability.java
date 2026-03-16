package com.warakorn.eternalx.modules.items.abilities;

import com.warakorn.eternalx.EternalX;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Base Abstract Class สำหรับ Ability ทุกชนิดของไอเท็ม
 */
public abstract class Ability {
  protected final EternalX plugin;
  protected final String id;
  protected final AbilityType type;
  protected final long cooldownTicks; // 20 ticks = 1 second
  protected final double manaCost;    // ค่า Mana ที่ต้องใช้ (0 = ไม่ใช้ mana)
  protected final double chance;      // โอกาสทำงาน 0.0-1.0 (1.0 = 100%)

  public Ability(EternalX plugin, String id, AbilityType type, Map<String, Object> config) {
    this.plugin = plugin;
    this.id = id;
    this.type = type;
    
    // โหลด cooldown พื้นฐาน ถ้า config มีก็ใช้ config, ถ้าไม่มีใช้ default 0
    if (config != null && config.containsKey("cooldown")) {
      this.cooldownTicks = ((Number) config.get("cooldown")).longValue();
    } else {
      this.cooldownTicks = 0;
    }

    // โหลด mana-cost
    this.manaCost = getDoubleConfig(config, "mana-cost", 0);

    // โหลด chance (โอกาสทำงาน, default = 1.0 คือ 100%)
    this.chance = getDoubleConfig(config, "chance", 1.0);
  }

  /**
   * เริ่มการทำงานของ Ability
   * @param player ผู้เล่นที่ใช้
   * @param item ไอเท็มที่ถืออยู่
   * @param event Event ที่ trigger ให้เกิด (อาจจะเป็น EntityDamageByEntityEvent, PlayerInteractEvent ฯลฯ)
   * @return true ถ้าใช้สำเร็จ (เพื่อจับ Cooldown), false ถ้าเงื่อนไขไม่ครบ
   */
  public abstract boolean execute(Player player, ItemStack item, Event event);

  public String getId() {
    return id;
  }

  public AbilityType getType() {
    return type;
  }

  public long getCooldownTicks() {
    return cooldownTicks;
  }

  public double getManaCost() {
    return manaCost;
  }

  public double getChance() {
    return chance;
  }

  /**
   * ช่วยดึงค่า Config แบบง่าย ป้องกัน null/casting error แบบต่างๆ
   */
  protected double getDoubleConfig(Map<String, Object> config, String key, double def) {
    if (config == null || !config.containsKey(key)) return def;
    Object val = config.get(key);
    if (val instanceof Number) return ((Number) val).doubleValue();
    try {
      return Double.parseDouble(val.toString());
    } catch (Exception e) {
      return def;
    }
  }

  protected int getIntConfig(Map<String, Object> config, String key, int def) {
    if (config == null || !config.containsKey(key)) return def;
    Object val = config.get(key);
    if (val instanceof Number) return ((Number) val).intValue();
    try {
      return Integer.parseInt(val.toString());
    } catch (Exception e) {
      return def;
    }
  }

  protected boolean getBooleanConfig(Map<String, Object> config, String key, boolean def) {
    if (config == null || !config.containsKey(key)) return def;
    Object val = config.get(key);
    if (val instanceof Boolean) return (Boolean) val;
    return Boolean.parseBoolean(val.toString());
  }
}

