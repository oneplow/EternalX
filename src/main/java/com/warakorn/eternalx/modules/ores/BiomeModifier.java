package com.warakorn.eternalx.modules.ores;

import com.warakorn.eternalx.EternalX;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BiomeModifier {

  private final EternalX plugin;
  private final OreManager config;

  public BiomeModifier(EternalX plugin) {
    this.plugin = plugin;
    this.config = plugin.getOreManager();
  }

  @SuppressWarnings("unchecked")
  public int modifyAllBiomes() {
    if (Bukkit.getWorlds().isEmpty()) {
      return 0;
    }

    org.bukkit.World bukkitWorld = Bukkit.getWorlds().get(0);

    try {
      Object nmsLevel = ReflectionUtil.getNMSLevel(bukkitWorld);
      RegistryAccess registryAccess = ReflectionUtil.getRegistryAccessFromLevel(nmsLevel);

      // Key สำหรับ 1.21
      ResourceKey<Registry<Biome>> biomeKey = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath("minecraft", "worldgen/biome")
      );

      Optional<Registry<Biome>> registryOpt = registryAccess.lookup(biomeKey);

      // Fallback เผื่อหาไม่เจอ
      if (registryOpt.isEmpty()) {
        registryOpt = registryAccess.lookup(ResourceKey.createRegistryKey(
          ResourceLocation.fromNamespaceAndPath("minecraft", "biome")
        ));
      }

      if (registryOpt.isEmpty()) {
        plugin.getLogger().severe("Could not find Biome Registry.");
        return 0;
      }

      Registry<Biome> biomeRegistry = registryOpt.get();
      int count = 0;
      int totalBiomes = 0;

      for (Object obj : biomeRegistry) {
        totalBiomes++;
        Biome biome = extractBiome(obj);

        if (biome != null) {
          if (modifySingleBiome(biome)) {
            count++;
          }
        }
      }

      plugin.getLogger().info("Success! CoreOres modified " + count + " out of " + totalBiomes + " biomes found.");
      return count;

    } catch (Exception e) {
      plugin.getLogger().severe("Error modifying biomes: " + e.getMessage());
      e.printStackTrace();
      return 0;
    }
  }

  private Biome extractBiome(Object obj) {
    if (obj == null) return null;
    if (obj instanceof Biome) return (Biome) obj;
    if (obj instanceof Holder) return (Biome) ((Holder<?>) obj).value();

    try {
      Method valueMethod = obj.getClass().getMethod("value");
      return (Biome) valueMethod.invoke(obj);
    } catch (Exception ignored) {
    }

    try {
      Method aMethod = obj.getClass().getMethod("a");
      return (Biome) aMethod.invoke(obj);
    } catch (Exception ignored) {
    }

    return null;
  }

  private boolean modifySingleBiome(Biome biome) {
    try {
      BiomeGenerationSettings settings = ReflectionUtil.getGenerationSettings(biome);
      List<Object> immutableFeatureSteps = ReflectionUtil.getFeaturesList(settings);

      if (immutableFeatureSteps == null || immutableFeatureSteps.isEmpty()) return false;

      int oreStepIndex = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
      if (oreStepIndex >= immutableFeatureSteps.size()) return false;

      // สร้าง Mutable List (หัวใจสำคัญที่แก้บั๊ก UnsupportedOperationException)
      List<Object> mutableFeatureSteps = new ArrayList<>(immutableFeatureSteps);

      Object stepObj = mutableFeatureSteps.get(oreStepIndex);
      if (!(stepObj instanceof HolderSet)) return false;

      @SuppressWarnings("unchecked")
      HolderSet<PlacedFeature> originalOres = (HolderSet<PlacedFeature>) stepObj;

      if (originalOres.size() == 0) return false;

      List<Holder<PlacedFeature>> filteredOres = new ArrayList<>();

      // ✅ Fix: ใช้ deterministic logic แทน Math.random()
      // rate >= 1.0 = เก็บไว้เสมอ, rate <= 0 = ลบทิ้งเสมอ
      // rate 0.0-1.0 = ใช้ hash ของ feature name เพื่อตัดสินใจอย่าง consistent
      originalOres.stream().forEach(holder -> {
        String featureKey = getFeatureKey(holder);
        double rate = config.getRate(featureKey);

        if (plugin.getSettingsManager().isDebugMode()) {
          plugin.getLogger().info("[Debug] " + featureKey + " -> rate=" + rate);
        }

        if (rate >= 1.0) {
          filteredOres.add(holder);
        } else if (rate > 0.0) {
          // Deterministic: ใช้ hash ของ featureKey เพื่อให้ผลเหมือนเดิมทุก restart
          double hash = (Math.abs(featureKey.hashCode()) % 10000) / 10000.0;
          if (hash < rate) {
            filteredOres.add(holder);
          }
        }
        // rate <= 0 = ไม่เพิ่ม = ลบออก
      });

      HolderSet<PlacedFeature> newOreSet = HolderSet.direct(filteredOres);
      mutableFeatureSteps.set(oreStepIndex, newOreSet);

      // Save กลับเข้าไป
      ReflectionUtil.setFeaturesList(settings, mutableFeatureSteps);

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String getFeatureKey(Holder<PlacedFeature> holder) {
    return holder.unwrapKey()
      .map(key -> key.location().getPath())
      .orElse("unknown_feature");
  }
}
