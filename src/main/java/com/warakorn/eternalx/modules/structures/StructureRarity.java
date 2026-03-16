package com.warakorn.eternalx.modules.structures;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * ระดับความหายากของโครงสร้าง
 * ยิ่ง rarity สูง = spawn ยากขึ้น + treasure ดีขึ้น
 */
public enum StructureRarity {
  COMMON(1.0, 1.0, "§7Common", NamedTextColor.GRAY),
  UNCOMMON(0.6, 1.2, "§aUncommon", NamedTextColor.GREEN),
  RARE(0.3, 1.5, "§9Rare", NamedTextColor.BLUE),
  EPIC(0.15, 2.0, "§5Epic", NamedTextColor.DARK_PURPLE),
  LEGENDARY(0.05, 3.0, "§6Legendary", NamedTextColor.GOLD),
  MYTHIC(0.01, 5.0, "§cMythic", NamedTextColor.RED);

  private final double spawnWeight;
  private final double treasureMultiplier;
  private final String displayName;
  private final NamedTextColor adventureColor; // ✅ Feature 5

  StructureRarity(double spawnWeight, double treasureMultiplier, String displayName, NamedTextColor adventureColor) {
    this.spawnWeight = spawnWeight;
    this.treasureMultiplier = treasureMultiplier;
    this.displayName = displayName;
    this.adventureColor = adventureColor;
  }

  public double getSpawnWeight() {
    return spawnWeight;
  }

  public double getTreasureMultiplier() {
    return treasureMultiplier;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getColorCode() {
    return displayName.substring(0, 2);
  }

  /**
   * ✅ Feature 5: Adventure API color
   */
  public NamedTextColor getAdventureColor() {
    return adventureColor;
  }

  public static StructureRarity fromFolder(String folderName) {
    try {
      return valueOf(folderName.toUpperCase());
    } catch (IllegalArgumentException e) {
      return COMMON;
    }
  }

  public String getSuggestedTreasureTier() {
    return switch (this) {
      case COMMON -> "common";
      case UNCOMMON -> "uncommon";
      case RARE -> "rare";
      case EPIC -> "epic";
      case LEGENDARY -> "legendary";
      case MYTHIC -> "mythic";
    };
  }

  public double calculateFinalWeight(double baseWeight) {
    return baseWeight * spawnWeight;
  }
}
