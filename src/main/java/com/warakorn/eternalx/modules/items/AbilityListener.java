package com.warakorn.eternalx.modules.items;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ดักจับ Event ต่างๆ แล้วยิงไปหา Ability ของไอเท็มที่ผู้เล่นถือ/สวมใส่
 */
public class AbilityListener implements Listener {
  private final EternalX plugin;

  // ป้องกัน infinite loop เวลา ability เรียก damage() แล้ว trigger event ซ้ำ
  private final Set<UUID> processingPlayers = new HashSet<>();

  public AbilityListener(EternalX plugin) {
    this.plugin = plugin;
    // Start passive tick check (Every 10 ticks = 0.5s)
    startPassiveAbilityTask();
  }

  // ==================== ON_HIT ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onEntityHit(EntityDamageByEntityEvent event) {
    Player player;

    // ตรวจสอบว่า damager เป็น Player โดยตรง หรือเป็น Projectile ที่ Player ยิง
    if (event.getDamager() instanceof Player p) {
      player = p;
    } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
      player = shooter;
    } else {
      return;
    }

    // ป้องกัน recursive loop: ถ้าผู้เล่นกำลัง process ability อยู่ ข้ามไป
    if (processingPlayers.contains(player.getUniqueId())) return;

    ItemStack mainHand = player.getInventory().getItemInMainHand();
    processingPlayers.add(player.getUniqueId());
    try {
      processAbilities(player, mainHand, AbilityType.ON_HIT, event);
    } finally {
      processingPlayers.remove(player.getUniqueId());
    }
  }

  // ==================== RIGHT_CLICK ====================
  @EventHandler(priority = EventPriority.NORMAL)
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer();
    ItemStack mainHand = player.getInventory().getItemInMainHand();
    processAbilities(player, mainHand, AbilityType.RIGHT_CLICK, event);
  }

  // ==================== ON_KILL ====================
  @EventHandler(priority = EventPriority.NORMAL)
  public void onEntityKill(EntityDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    if (killer == null) return;

    ItemStack mainHand = killer.getInventory().getItemInMainHand();
    processAbilities(killer, mainHand, AbilityType.ON_KILL, event);
  }

  // ==================== ON_DAMAGE_TAKEN ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onDamageTaken(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;

    // เช็คของทั้งตัว (เกราะ + มืด) เพราะพวกเกราะมักจะมีสกิลเวลารับดาเมจ
    processArmorAndHands(player, AbilityType.ON_DAMAGE_TAKEN, event);
  }

  // ==================== DOUBLE JUMP (SPECIAL) ====================
  @EventHandler(priority = EventPriority.NORMAL)
  public void onToggleFlight(org.bukkit.event.player.PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();
    // เราไม่ได้มี Trigger แยกสำหรับอันนี้ เลยใช้ PASSIVE เพื่อกระจาย Event แทรก
    processArmorAndHands(player, AbilityType.PASSIVE, event);
  }

  // ==================== SNEAK ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onToggleSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
    if (!event.isSneaking()) return; // เฉพาะตอนกด shift ลง

    Player player = event.getPlayer();
    processArmorAndHands(player, AbilityType.SNEAK, event);
  }

  // ==================== JUMP ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerJump(PlayerJumpEvent event) {
    Player player = event.getPlayer();
    processArmorAndHands(player, AbilityType.JUMP, event);
  }

  // ==================== FISH (Fishing Rod Hook) ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    // ทำงานเมื่อเบ็ดตกถึงจุดหมาย (ไม่ใช่ตอนปล่อยสาย)
    if (event.getState() != PlayerFishEvent.State.IN_GROUND
        && event.getState() != PlayerFishEvent.State.REEL_IN
        && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;

    Player player = event.getPlayer();
    ItemStack mainHand = player.getInventory().getItemInMainHand();
    processAbilities(player, mainHand, AbilityType.FISH, event);
  }

  // ==================== AREA EFFECT CLOUD PROTECTION ====================
  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onCloudApply(AreaEffectCloudApplyEvent event) {
    AreaEffectCloud cloud = event.getEntity();
    if (cloud.getSource() instanceof Player caster) {
      // ลบผู้สร้างออกจากรายชื่อคนที่จะโดน effect → ไม่โดนควันตัวเอง
      event.getAffectedEntities().removeIf(entity -> entity.getUniqueId().equals(caster.getUniqueId()));
    }
  }

  // ==================== PASSIVE TASK ====================
  private void startPassiveAbilityTask() {
    plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
      for (Player player : plugin.getServer().getOnlinePlayers()) {
        processArmorAndHands(player, AbilityType.PASSIVE, null);
      }
    }, 20L, 20L); // เช็คทุกๆ 20 tick (1 วินาที)
  }

  // ==================== PROCESS ====================

  private void processArmorAndHands(Player player, AbilityType type, Event event) {
    // Main Hand
    processAbilities(player, player.getInventory().getItemInMainHand(), type, event);
    // Off Hand
    processAbilities(player, player.getInventory().getItemInOffHand(), type, event);

    // Armor
    ItemStack[] armor = player.getInventory().getArmorContents();
    for (ItemStack a : armor) {
      if (a != null) {
        processAbilities(player, a, type, event);
      }
    }
  }

  private void processAbilities(Player player, ItemStack item, AbilityType triggerType, Event event) {
    if (!plugin.getCustomItemManager().isCustomItem(item)) return;

    String itemId = plugin.getCustomItemManager().getCustomItemId(item);
    CustomItem customItem = plugin.getCustomItemManager().getCustomItem(itemId);
    if (customItem == null) return;

    List<Map<String, Object>> rawAbilities = customItem.getRawAbilities();
    if (rawAbilities == null || rawAbilities.isEmpty()) return;

    for (Map<String, Object> rawAb : rawAbilities) {
      String abTypeStr = (String) rawAb.get("type");
      AbilityType pType;
      try {
        pType = AbilityType.valueOf(abTypeStr.toUpperCase());
      } catch (Exception e) {
        continue; // ผิดประเภท
      }

      if (pType != triggerType) continue; // ข้ามถ้าประเภท Trigger ไม่ตรง

      String abilityName = (String) rawAb.get("ability");
      if (abilityName == null) continue;

      // สร้าง ability ชั่วคราวจาก Registry เพื่อ execute
      Ability ability = plugin.getAbilityRegistry().createAbility(abilityName, rawAb);
      if (ability == null) continue;

      // เช็ค Cooldown
      String cooldownKey = itemId + "_" + abilityName;
      if (plugin.getCooldownManager().isOnCooldown(player, cooldownKey)) {
        if (triggerType == AbilityType.RIGHT_CLICK || triggerType == AbilityType.SNEAK || triggerType == AbilityType.JUMP) {
          plugin.getCooldownManager().sendCooldownMessage(player, cooldownKey);
        }
        continue;
      }

      // เช็ค Chance (โอกาสทำงาน) - ถ้าไม่ผ่าน ข้ามไปเลย (ไม่แจ้งข้อความ)
      if (ability.getChance() < 1.0 && Math.random() > ability.getChance()) {
        continue;
      }

      // เช็ค Mana
      if (ability.getManaCost() > 0) {
        if (!plugin.getManaManager().hasMana(player, ability.getManaCost())) {
          if (triggerType == AbilityType.RIGHT_CLICK || triggerType == AbilityType.SNEAK || triggerType == AbilityType.JUMP) {
            plugin.getManaManager().sendNotEnoughManaMessage(player, ability.getManaCost());
          }
          continue;
        }
      }

      // Execute 
      boolean success = ability.execute(player, item, event);

      // ถ้าทำสำเร็จ → หัก Mana + ใส่ Cooldown
      if (success) {
        if (ability.getManaCost() > 0) {
          plugin.getManaManager().consumeMana(player, ability.getManaCost());
        }
        if (ability.getCooldownTicks() > 0) {
          plugin.getCooldownManager().setCooldown(player, cooldownKey, ability.getCooldownTicks());
        }
      }
    }
  }
}
