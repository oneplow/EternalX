package com.warakorn.eternalx.modules.structures;

/**
 * ระดับความหายากของโครงสร้าง
 * ยิ่ง rarity สูง = spawn ยากขึ้น + treasure ดีขึ้น
 */
public enum StructureRarity {
  COMMON(1.0, 1.0, "§7Common"),          // spawn บ่อย, treasure ธรรมดา
  UNCOMMON(0.6, 1.2, "§aUncommon"),      // spawn ค่อนข้างบ่อย, treasure ดีขึ้นเล็กน้อย
  RARE(0.3, 1.5, "§9Rare"),              // spawn ปานกลาง, treasure ดี
  EPIC(0.15, 2.0, "§5Epic"),             // spawn ยาก, treasure ดีมาก
  LEGENDARY(0.05, 3.0, "§6Legendary"),   // spawn ยากมาก, treasure เทพ
  MYTHIC(0.01, 5.0, "§cMythic");         // spawn หายาก, treasure สุดยอด

  private final double spawnWeight;        // น้ำหนักการ spawn (ยิ่งต่ำยิ่งหายาก)
  private final double treasureMultiplier; // ตัวคูณคุณภาพของ treasure
  private final String displayName;        // ชื่อแสดงผล

  StructureRarity(double spawnWeight, double treasureMultiplier, String displayName) {
    this.spawnWeight = spawnWeight;
    this.treasureMultiplier = treasureMultiplier;
    this.displayName = displayName;
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
    return displayName.substring(0, 2); // ดึงแค่ §X
  }

  /**
   * แปลงชื่อ folder เป็น Rarity
   * เช่น "common" -> COMMON
   */
  public static StructureRarity fromFolder(String folderName) {
    try {
      return valueOf(folderName.toUpperCase());
    } catch (IllegalArgumentException e) {
      return COMMON; // default ถ้าไม่เจอ
    }
  }

  /**
   * เช็คว่า rarity นี้ควรได้ treasure tier ไหน
   * ใช้สำหรับเลือก treasure pool
   */
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

  /**
   * คำนวณโอกาสจริงที่จะ spawn
   * ใช้ร่วมกับ weight จาก config
   */
  public double calculateFinalWeight(double baseWeight) {
    return baseWeight * spawnWeight;
  }
}
