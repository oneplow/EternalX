package com.warakorn.eternalx.modules.items.abilities;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.CustomItem;
import org.bukkit.event.Event;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * เก็บการลงทะเบียนของ Ability ทั้งหมด
 * Factory Pattern 
 */
public class AbilityRegistry {
  private final EternalX plugin;
  private final Map<String, Class<? extends Ability>> registry;

  public AbilityRegistry(EternalX plugin) {
    this.plugin = plugin;
    this.registry = new HashMap<>();
    registerBuiltInAbilities();
  }

  /**
   * ลงทะเบียน Ability
   */
  public void register(String id, Class<? extends Ability> clazz) {
    registry.put(id.toUpperCase(), clazz);
  }

  /**
   * สร้าง Instance ของ Ability จากชื่อ/Config ที่ระบุใน YAML
   */
  public Ability createAbility(String id, Map<String, Object> config) {
    Class<? extends Ability> clazz = registry.get(id.toUpperCase());
    if (clazz == null) {
      // Passives are special cases
      if (id.equalsIgnoreCase("NIGHT_VISION")) return new com.warakorn.eternalx.modules.items.abilities.passive.PassiveEffectAbility(plugin, id, config, org.bukkit.potion.PotionEffectType.NIGHT_VISION);
      if (id.equalsIgnoreCase("HASTE")) return new com.warakorn.eternalx.modules.items.abilities.passive.PassiveEffectAbility(plugin, id, config, org.bukkit.potion.PotionEffectType.HASTE);
      if (id.equalsIgnoreCase("SATURATION")) return new com.warakorn.eternalx.modules.items.abilities.passive.PassiveEffectAbility(plugin, id, config, org.bukkit.potion.PotionEffectType.SATURATION);

      plugin.getLogger().warning("Unknown ability: " + id);
      return null;
    }

    try {
      Constructor<? extends Ability> constructor = clazz.getConstructor(EternalX.class, String.class, Map.class);
      return constructor.newInstance(plugin, id, config);
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to instantiate ability: " + id);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * ลงทะเบียน Built-in Abilities ทั้งหมด
   */
  private void registerBuiltInAbilities() {
    // ⚔️ Combat
    register("LIFESTEAL", com.warakorn.eternalx.modules.items.abilities.combat.LifestealAbility.class);
    register("WITHER_TOUCH", com.warakorn.eternalx.modules.items.abilities.combat.WitherTouchAbility.class);
    register("BLEED", com.warakorn.eternalx.modules.items.abilities.combat.BleedAbility.class);
    register("EXECUTE", com.warakorn.eternalx.modules.items.abilities.combat.ExecuteAbility.class);
    register("SWEEPING_WAVE", com.warakorn.eternalx.modules.items.abilities.combat.SweepingWaveAbility.class);
    register("GROUND_SLAM", com.warakorn.eternalx.modules.items.abilities.combat.GroundSlamAbility.class);
    register("BACKSTAB", com.warakorn.eternalx.modules.items.abilities.combat.BackstabAbility.class);
    register("CHAIN_LIGHTNING", com.warakorn.eternalx.modules.items.abilities.combat.ChainLightningAbility.class);

    // 🛡️ Defense
    register("RESURRECT", com.warakorn.eternalx.modules.items.abilities.defense.ResurrectAbility.class);
    register("DAMAGE_REFLECT", com.warakorn.eternalx.modules.items.abilities.defense.DamageReflectAbility.class);
    register("SHIELD_WALL", com.warakorn.eternalx.modules.items.abilities.defense.ShieldWallAbility.class);
    register("ABSORPTION", com.warakorn.eternalx.modules.items.abilities.defense.AbsorptionAbility.class);
    register("COUNTER_STRIKE", com.warakorn.eternalx.modules.items.abilities.defense.CounterStrikeAbility.class);

    // 🏃 Movement
    register("DASH", com.warakorn.eternalx.modules.items.abilities.movement.DashAbility.class);
    register("GRAPPLE", com.warakorn.eternalx.modules.items.abilities.movement.GrappleAbility.class);
    register("BLINK", com.warakorn.eternalx.modules.items.abilities.movement.BlinkAbility.class);
    register("DOUBLE_JUMP", com.warakorn.eternalx.modules.items.abilities.movement.DoubleJumpAbility.class);
    register("SPEED_BURST", com.warakorn.eternalx.modules.items.abilities.movement.SpeedBurstAbility.class);

    // 🔧 Utility
    register("MAGNET", com.warakorn.eternalx.modules.items.abilities.utility.MagnetAbility.class);
    register("HEAL_AURA", com.warakorn.eternalx.modules.items.abilities.utility.HealAuraAbility.class);
    register("LIGHT_ORB", com.warakorn.eternalx.modules.items.abilities.utility.LightOrbAbility.class);
    register("SMELTING_TOUCH", com.warakorn.eternalx.modules.items.abilities.utility.SmeltingTouchAbility.class);
    register("TELEPATHY", com.warakorn.eternalx.modules.items.abilities.utility.TelepathyAbility.class);

    // 🌊 Elemental
    register("IGNITE", com.warakorn.eternalx.modules.items.abilities.elemental.IgniteAbility.class);
    register("FREEZE", com.warakorn.eternalx.modules.items.abilities.elemental.FreezeAbility.class);
    register("LIGHTNING", com.warakorn.eternalx.modules.items.abilities.elemental.LightningAbility.class);
    register("POISON_CLOUD", com.warakorn.eternalx.modules.items.abilities.elemental.PoisonCloudAbility.class);
  }
}
