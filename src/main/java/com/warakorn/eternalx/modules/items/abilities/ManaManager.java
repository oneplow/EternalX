package com.warakorn.eternalx.modules.items.abilities;

import com.warakorn.eternalx.EternalX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * จัดการระบบ Mana สำหรับผู้เล่น
 * - เก็บ Mana ปัจจุบันของแต่ละคน
 * - ฟื้นฟู Mana อัตโนมัติตามเวลา
 * - แสดงผลผ่าน XP Bar (Level = Mana, Progress = %)
 */
public class ManaManager {
  private final EternalX plugin;

  private final Map<UUID, Double> playerMana = new HashMap<>();

  // ค่า Config
  private double maxMana;
  private double regenPerSecond;
  private long regenIntervalTicks;

  public ManaManager(EternalX plugin) {
    this.plugin = plugin;
    loadConfig();
    startRegenTask();
  }

  /**
   * โหลดค่า Config จาก config.yml
   */
  public void loadConfig() {
    this.maxMana = plugin.getConfig().getDouble("mana.max-mana", 100.0);
    this.regenPerSecond = plugin.getConfig().getDouble("mana.regen-per-second", 2.0);
    this.regenIntervalTicks = plugin.getConfig().getLong("mana.regen-interval-ticks", 20L);
  }

  /**
   * ดึง Mana ปัจจุบัน
   */
  public double getMana(Player player) {
    return playerMana.getOrDefault(player.getUniqueId(), maxMana);
  }

  /**
   * เช็คว่ามี Mana พอใช้ไหม
   */
  public boolean hasMana(Player player, double cost) {
    return getMana(player) >= cost;
  }

  /**
   * หัก Mana เมื่อใช้ Ability
   */
  public void consumeMana(Player player, double cost) {
    double current = getMana(player);
    double newMana = Math.max(0, current - cost);
    playerMana.put(player.getUniqueId(), newMana);
    updateManaDisplay(player);
  }

  /**
   * ฟื้นฟู Mana
   */
  public void regenMana(Player player, double amount) {
    double current = getMana(player);
    double newMana = Math.min(maxMana, current + amount);
    playerMana.put(player.getUniqueId(), newMana);
    updateManaDisplay(player);
  }

  /**
   * ตั้งค่า Mana ตรงๆ
   */
  public void setMana(Player player, double amount) {
    playerMana.put(player.getUniqueId(), Math.min(maxMana, Math.max(0, amount)));
    updateManaDisplay(player);
  }

  /**
   * รีเซ็ต Mana ให้เต็ม
   */
  public void resetMana(Player player) {
    playerMana.put(player.getUniqueId(), maxMana);
    updateManaDisplay(player);
  }

  /**
   * ล้างข้อมูลของผู้เล่นที่ออก
   */
  public void removePlayer(Player player) {
    playerMana.remove(player.getUniqueId());
  }

  /**
   * ล้างข้อมูลทั้งหมด (ใช้ตอน Reload)
   */
  public void clearAll() {
    playerMana.clear();
  }

  public double getMaxMana() {
    return maxMana;
  }

  /**
   * อัปเดต Action Bar ให้แสดง Mana
   * แสดงเป็นแถบ visual เช่น: ⚡ Mana: ████████░░ 80/100
   */
  public void updateManaDisplay(Player player) {
    double mana = getMana(player);
    int manaInt = (int) Math.floor(mana);
    int maxManaInt = (int) Math.floor(maxMana);

    // สร้างแถบ visual 10 ช่อง
    int totalBars = 10;
    int filledBars = (int) Math.round((mana / maxMana) * totalBars);
    filledBars = Math.max(0, Math.min(totalBars, filledBars));

    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < filledBars; i++) bar.append("█");
    for (int i = filledBars; i < totalBars; i++) bar.append("░");

    // สีตาม % ของ mana
    TextColor manaColor;
    double percent = mana / maxMana;
    if (percent > 0.5) {
      manaColor = TextColor.color(85, 170, 255);  // ฟ้า
    } else if (percent > 0.25) {
      manaColor = TextColor.color(255, 170, 0);    // ส้ม
    } else {
      manaColor = TextColor.color(255, 85, 85);    // แดง
    }

    player.sendActionBar(
        Component.text("⚡ Mana: " + bar + " " + manaInt + "/" + maxManaInt, manaColor)
    );
  }

  /**
   * เริ่ม Task ฟื้นฟู Mana อัตโนมัติ
   */
  private void startRegenTask() {
    double regenAmount = regenPerSecond * (regenIntervalTicks / 20.0);

    new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
          double current = getMana(player);
          if (current < maxMana) {
            regenMana(player, regenAmount);
          }
        }
      }
    }.runTaskTimer(plugin, regenIntervalTicks, regenIntervalTicks);
  }

  /**
   * ส่งข้อความบอกว่า Mana ไม่พอ
   */
  public void sendNotEnoughManaMessage(Player player, double cost) {
    double current = getMana(player);
    player.sendActionBar(
        Component.text(String.format("⚡ Not enough mana! (%.0f/%.0f, need %.0f)", current, maxMana, cost),
            TextColor.color(85, 170, 255))
    );
  }
}
