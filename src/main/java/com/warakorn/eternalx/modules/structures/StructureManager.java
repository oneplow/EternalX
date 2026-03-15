package com.warakorn.eternalx.modules.structures;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.warakorn.eternalx.EternalX;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Collections;

public class StructureManager {
  private final EternalX plugin;
  private final List<StructureData> loadedStructures = new ArrayList<>();

  public StructureManager(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * ⭐ โหลดโครงสร้างจาก folder structure แบบใหม่
   * imports/
   *   common/     <- StructureRarity.COMMON
   *   uncommon/   <- StructureRarity.UNCOMMON
   *   rare/       <- StructureRarity.RARE
   *   epic/       <- StructureRarity.EPIC
   *   legendary/  <- StructureRarity.LEGENDARY
   *   mythic/     <- StructureRarity.MYTHIC
   */
  public void loadStructures() {
    loadedStructures.clear();
    File importsDir = plugin.getSettingsManager().getImportsFolder();
    File structuresDir = plugin.getSettingsManager().getStructuresFolder();

    // สร้าง rarity folders ถ้ายังไม่มี
    for (StructureRarity rarity : StructureRarity.values()) {
      File rarityFolder = new File(importsDir, rarity.name().toLowerCase());
      if (!rarityFolder.exists()) {
        rarityFolder.mkdirs();
        plugin.getLogger().info("Created rarity folder: " + rarityFolder.getName());
      }
    }

    // สแกนทุก rarity folder
    for (StructureRarity rarity : StructureRarity.values()) {
      File rarityFolder = new File(importsDir, rarity.name().toLowerCase());
      loadStructuresFromRarityFolder(rarityFolder, rarity, structuresDir);
    }

    plugin.getLogger().info("§aLoaded §e" + loadedStructures.size() + " §astructures:");
    
    // แสดงสถิติตาม rarity
    Map<StructureRarity, Long> countByRarity = loadedStructures.stream()
      .collect(Collectors.groupingBy(StructureData::getRarity, Collectors.counting()));
    
    for (StructureRarity rarity : StructureRarity.values()) {
      long count = countByRarity.getOrDefault(rarity, 0L);
      if (count > 0) {
        plugin.getLogger().info("  " + rarity.getDisplayName() + "§r: §e" + count + " §7structures");
      }
    }
  }

  /**
   * โหลดโครงสร้างจาก rarity folder เฉพาะ
   */
  private void loadStructuresFromRarityFolder(File rarityFolder, StructureRarity rarity, File structuresDir) {
    if (!rarityFolder.exists() || !rarityFolder.isDirectory()) {
      return;
    }

    File[] schemFiles = rarityFolder.listFiles((dir, name) -> 
      name.endsWith(".schem") || name.endsWith(".schematic"));

    if (schemFiles == null || schemFiles.length == 0) {
      return;
    }

    // ⭐ สร้าง rarity folder ใน structures/ ด้วย
    File rarityConfigFolder = new File(structuresDir, rarity.name().toLowerCase());
    if (!rarityConfigFolder.exists()) {
      rarityConfigFolder.mkdirs();
      plugin.getLogger().info("Created config folder: structures/" + rarity.name().toLowerCase());
    }

    for (File schemFile : schemFiles) {
      String structureName = schemFile.getName()
        .replace(".schem", "")
        .replace(".schematic", "");
      
      // ⭐ Config file อยู่ใน structures/{rarity}/ โดยใช้ชื่อ: structurename.yml
      File configFile = new File(rarityConfigFolder, structureName + ".yml");

      if (!configFile.exists()) {
        createDefaultConfig(configFile, schemFile.getName(), rarity);
        plugin.getLogger().info("Generated config: structures/" + rarity.name().toLowerCase() + "/" + structureName + ".yml");
      }
      
      loadSingleStructure(structureName, configFile, schemFile, rarity);
    }
  }

  /**
   * สร้าง config เริ่มต้นที่เหมาะสมกับ rarity
   */
  private void createDefaultConfig(File configFile, String schemFileName, StructureRarity rarity) {
    try {
      configFile.createNewFile();
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

      config.set("file", schemFileName);
      config.set("rarity", rarity.name());
      
      // Weight: ยิ่ง rarity สูง ยิ่ง weight น้อย (แต่ระบบจะคูณด้วย rarity.spawnWeight อีกที)
      config.set("weight", 1.0);
      
      // Spacing: ยิ่ง rarity สูง ยิ่งห่างกัน
      int spacing = switch (rarity) {
        case COMMON -> 15;
        case UNCOMMON -> 20;
        case RARE -> 30;
        case EPIC -> 40;
        case LEGENDARY -> 60;
        case MYTHIC -> 80;
      };
      config.set("spacing", spacing);
      
      config.set("offset-y", 1);

      // Rotation
      config.set("rotation.mode", "RANDOM");
      config.set("rotation.enforce-horizontal", true);
      config.set("rotation.enforce-vertical", true);
      config.set("rotation.allowed-angles", Arrays.asList(0, 90, 180, 270));

      config.set("paste-air", false);
      config.set("placement-type", "SURFACE");
      config.set("dimension", "OVERWORLD");

      // Biome
      config.set("biome.mode", "WHITELIST");
      config.set("biome.strict-check", false);
      config.set("biome.allowed", Arrays.asList("PLAINS", "FOREST", "BIRCH_FOREST"));
      config.set("biome.forbidden", Arrays.asList("DESERT", "SNOWY_TAIGA"));

      config.set("ground-blocks", Arrays.asList("GRASS_BLOCK", "DIRT", "PODZOL", "STONE"));

      // ⭐ Treasure: ใช้ชื่อตาม rarity (จะหา common_loot.yml, rare_loot.yml, etc.)
      config.set("treasure-file", rarity.getSuggestedTreasureTier() + "_loot");

      config.save(configFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * โหลดโครงสร้างเดี่ยว
   */
  private void loadSingleStructure(String id, File configFile, File schemFile, StructureRarity rarity) {
    try {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      String configuredFileName = config.getString("file", schemFile.getName());
      
      // ใช้ path เต็มของ schemFile แทน
      File targetSchem = schemFile;
      if (!targetSchem.exists()) {
        plugin.getLogger().warning("Missing schematic: " + schemFile.getAbsolutePath());
        return;
      }

      Clipboard clipboard = loadSchematic(targetSchem);
      if (clipboard == null) return;

      // ============ ค่าพื้นฐาน ============
      double weight = config.getDouble("weight", 1.0);
      int spacing = config.getInt("spacing", 20);
      int offsetY = config.getInt("offset-y", 0);
      boolean pasteAir = config.getBoolean("paste-air", false);

      // ============ Placement Type & Dimension ============
      PlacementType placementType;
      try {
        placementType = PlacementType.valueOf(config.getString("placement-type", "SURFACE").toUpperCase());
      } catch (IllegalArgumentException e) {
        placementType = PlacementType.SURFACE;
      }

      DimensionType dimensionType;
      try {
        dimensionType = DimensionType.valueOf(config.getString("dimension", "OVERWORLD").toUpperCase());
      } catch (IllegalArgumentException e) {
        dimensionType = DimensionType.OVERWORLD;
      }

      // ============ Rotation System ============
      RotationMode rotationMode;
      try {
        rotationMode = RotationMode.valueOf(config.getString("rotation.mode", "RANDOM").toUpperCase());
      } catch (IllegalArgumentException e) {
        rotationMode = RotationMode.RANDOM;
      }

      int[] allowedRotations = null;
      if (rotationMode == RotationMode.CUSTOM) {
        List<Integer> angles = config.getIntegerList("rotation.allowed-angles");
        if (!angles.isEmpty()) {
          allowedRotations = angles.stream().mapToInt(Integer::intValue).toArray();
        } else {
          allowedRotations = new int[]{0, 90, 180, 270};
        }
      }

      boolean enforceHorizontal = config.getBoolean("rotation.enforce-horizontal", true);
      boolean enforceVertical = config.getBoolean("rotation.enforce-vertical", true);

      // ============ Biome System ============
      BiomeMatchMode biomeMatchMode;
      try {
        biomeMatchMode = BiomeMatchMode.valueOf(config.getString("biome.mode", "WHITELIST").toUpperCase());
      } catch (IllegalArgumentException e) {
        biomeMatchMode = BiomeMatchMode.WHITELIST;
      }

      boolean strictBiomeCheck = config.getBoolean("biome.strict-check", false);

      Set<Biome> allowedBiomes = config.getStringList("biome.allowed").stream()
        .map(s -> {
          try {
            String keyStr = s.toLowerCase(Locale.ROOT);
            if (!keyStr.contains(":")) keyStr = "minecraft:" + keyStr;
            return Registry.BIOME.get(NamespacedKey.fromString(keyStr));
          } catch (Exception e) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

      Set<Biome> forbiddenBiomes = config.getStringList("biome.forbidden").stream()
        .map(s -> {
          try {
            String keyStr = s.toLowerCase(Locale.ROOT);
            if (!keyStr.contains(":")) keyStr = "minecraft:" + keyStr;
            return Registry.BIOME.get(NamespacedKey.fromString(keyStr));
          } catch (Exception e) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

      Set<Material> ground = config.getStringList("ground-blocks").stream()
        .map(Material::matchMaterial).filter(Objects::nonNull).collect(Collectors.toSet());

      // ============ Treasure ============
      String treasureFile = config.getString("treasure-file", null);

      // ⭐ อ่าน rarity จาก config (ถ้ามี) ไม่งั้นใช้จาก folder
      StructureRarity finalRarity;
      String rarityStr = config.getString("rarity");
      if (rarityStr != null) {
        try {
          finalRarity = StructureRarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
          finalRarity = rarity; // ใช้จาก folder
        }
      } else {
        finalRarity = rarity;
      }

      // ============ สร้าง StructureData ============
      loadedStructures.add(new StructureData(
        id, clipboard, weight, spacing, offsetY,
        allowedBiomes, forbiddenBiomes, ground,
        rotationMode != RotationMode.NONE, pasteAir,
        placementType, dimensionType,
        treasureFile, biomeMatchMode, strictBiomeCheck,
        rotationMode, allowedRotations, enforceHorizontal, enforceVertical,
        finalRarity
      ));

      plugin.debugLog("Loaded: §e" + id + " " + finalRarity.getDisplayName() +
        " §7[" + placementType + ", " + dimensionType + ", weight=" + 
        String.format("%.2f", finalRarity.calculateFinalWeight(weight)) + "]");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Clipboard loadSchematic(File file) {
    ClipboardFormat format = ClipboardFormats.findByFile(file);
    if (format == null) return null;
    try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
      return reader.read();
    } catch (IOException e) {
      return null;
    }
  }

  public List<StructureData> getStructures() {
    return Collections.unmodifiableList(loadedStructures);
  }

  public StructureData getStructure(String id) {
    for (StructureData data : loadedStructures) {
      if (data.getId().equalsIgnoreCase(id)) return data;
    }
    return null;
  }
}
