package com.warakorn.eternalx.modules.structures;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.Random;
import java.util.Set;

public class StructureData {
  private final String id;
  private final Clipboard clipboard;
  private final double weight;
  private final int spacing;
  private final int offsetY;
  private final Set<Biome> allowedBiomes;
  private final Set<Biome> forbiddenBiomes;
  private final Set<Material> validGround;
  private final boolean randomRotation;
  private final boolean pasteAir;
  private final PlacementType placementType;
  private final DimensionType dimensionType;

  // Treasure
  private final String treasureFile;

  // Biome-specific settings
  private final BiomeMatchMode biomeMatchMode;
  private final boolean strictBiomeCheck;

  // Rotation
  private final RotationMode rotationMode;
  private final int[] allowedRotations;
  private final boolean enforceHorizontal;
  private final boolean enforceVertical;

  // ⭐ ใหม่: Rarity System
  private final StructureRarity rarity;

  public StructureData(String id, Clipboard clipboard, double weight, int spacing, int offsetY,
                       Set<Biome> allowedBiomes, Set<Biome> forbiddenBiomes, Set<Material> validGround,
                       boolean randomRotation, boolean pasteAir,
                       PlacementType placementType, DimensionType dimensionType,
                       String treasureFile, BiomeMatchMode biomeMatchMode, boolean strictBiomeCheck, 
                       RotationMode rotationMode, int[] allowedRotations,
                       boolean enforceHorizontal, boolean enforceVertical, StructureRarity rarity) {
    this.id = id;
    this.clipboard = clipboard;
    this.weight = weight;
    this.spacing = spacing;
    this.offsetY = offsetY;
    this.allowedBiomes = allowedBiomes;
    this.forbiddenBiomes = forbiddenBiomes;
    this.validGround = validGround;
    this.randomRotation = randomRotation;
    this.pasteAir = pasteAir;
    this.placementType = placementType;
    this.dimensionType = dimensionType;
    this.treasureFile = treasureFile;
    this.biomeMatchMode = biomeMatchMode;
    this.strictBiomeCheck = strictBiomeCheck;
    this.rotationMode = rotationMode;
    this.allowedRotations = allowedRotations != null ? allowedRotations : new int[]{0};
    this.enforceHorizontal = enforceHorizontal;
    this.enforceVertical = enforceVertical;
    this.rarity = rarity != null ? rarity : StructureRarity.COMMON;
  }

  // คำนวณมุมหมุนตาม mode
  public int getRotation(Random random) {
    switch (rotationMode) {
      case NONE:
        return 0;
      case RANDOM:
        return random.nextInt(4) * 90;
      case CARDINAL_ONLY:
        return random.nextBoolean() ? 0 : 180;
      case CUSTOM:
        if (allowedRotations.length == 0) return 0;
        return allowedRotations[random.nextInt(allowedRotations.length)];
    }
    return 0;
  }

  /**
   * ⭐ คำนวณ spawn weight ที่รวม rarity แล้ว
   */
  public double getFinalWeight() {
    return rarity.calculateFinalWeight(weight);
  }

  /**
   * ⭐ ดึงชื่อ treasure file ที่เหมาะสมกับ rarity
   * ถ้าไม่มีกำหนดเอง จะใช้ชื่อจาก rarity
   */
  public String getEffectiveTreasureFile() {
    if (treasureFile != null && !treasureFile.isEmpty()) {
      return treasureFile;
    }
    // ใช้ชื่อจาก rarity เป็น default
    return rarity.getSuggestedTreasureTier() + "_loot";
  }

  // ===== Getters =====

  public StructureRarity getRarity() {
    return rarity;
  }

  public RotationMode getRotationMode() {
    return rotationMode;
  }

  public int[] getAllowedRotations() {
    return allowedRotations;
  }

  public boolean isEnforceHorizontal() {
    return enforceHorizontal;
  }

  public boolean isEnforceVertical() {
    return enforceVertical;
  }

  public Set<Biome> getForbiddenBiomes() {
    return forbiddenBiomes;
  }

  public String getTreasureFile() {
    return treasureFile;
  }

  public BiomeMatchMode getBiomeMatchMode() {
    return biomeMatchMode;
  }

  public boolean isStrictBiomeCheck() {
    return strictBiomeCheck;
  }

  // ตรวจสอบ biome แบบละเอียด
  public boolean canSpawnInBiome(Biome biome) {
    switch (biomeMatchMode) {
      case ANY:
        return true;
      case WHITELIST:
        return allowedBiomes.isEmpty() || allowedBiomes.contains(biome);
      case BLACKLIST:
        return !forbiddenBiomes.contains(biome);
      case STRICT:
        return allowedBiomes.contains(biome) && !forbiddenBiomes.contains(biome);
    }
    return true;
  }

  public String getId() {
    return id;
  }

  public Clipboard getClipboard() {
    return clipboard;
  }

  public double getWeight() {
    return weight;
  }

  public int getSpacing() {
    return spacing;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public Set<Biome> getAllowedBiomes() {
    return allowedBiomes;
  }

  public Set<Material> getValidGround() {
    return validGround;
  }

  public boolean isRandomRotation() {
    return randomRotation;
  }

  public boolean isPasteAir() {
    return pasteAir;
  }

  public PlacementType getPlacementType() {
    return placementType;
  }

  public DimensionType getDimensionType() {
    return dimensionType;
  }

  public boolean canSpawnInWorld(World world) {
    return dimensionType.matches(world);
  }

  public int getWidth() {
    return clipboard.getDimensions().x();
  }

  public int getLength() {
    return clipboard.getDimensions().z();
  }

  public int getHeight() {
    return clipboard.getDimensions().y();
  }
}
