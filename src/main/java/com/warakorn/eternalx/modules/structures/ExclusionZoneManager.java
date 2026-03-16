package com.warakorn.eternalx.modules.structures;

import com.warakorn.eternalx.EternalX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ป้องกัน structure ซ้อนทับกัน
 * เก็บ occupied chunks per world และเช็คก่อน spawn ใหม่
 */
public class ExclusionZoneManager {
  private final EternalX plugin;
  // worldName → set of encoded chunk coords
  private final Map<String, Set<Long>> occupiedChunks = new ConcurrentHashMap<>();

  public ExclusionZoneManager(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * Encode (chunkX, chunkZ) เป็น long เดียว
   */
  private static long encodeChunk(int chunkX, int chunkZ) {
    return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
  }

  /**
   * เช็คว่า chunk (และ radius รอบๆ) ว่างอยู่ไหม
   * @param radius จำนวน chunks รอบๆ ที่ต้องว่าง
   */
  public boolean isOccupied(String worldName, int chunkX, int chunkZ, int radius) {
    Set<Long> worldSet = occupiedChunks.get(worldName);
    if (worldSet == null || worldSet.isEmpty()) return false;

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        if (worldSet.contains(encodeChunk(chunkX + dx, chunkZ + dz))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Mark chunks ว่ามี structure แล้ว
   * @param radius จำนวน chunks ที่ถูก mark
   */
  public void markOccupied(String worldName, int chunkX, int chunkZ, int radius) {
    Set<Long> worldSet = occupiedChunks.computeIfAbsent(worldName, k -> ConcurrentHashMap.newKeySet());

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        worldSet.add(encodeChunk(chunkX + dx, chunkZ + dz));
      }
    }

    plugin.debugLog("Exclusion zone marked: chunks (" + 
      (chunkX - radius) + "," + (chunkZ - radius) + ") to (" + 
      (chunkX + radius) + "," + (chunkZ + radius) + ") in " + worldName);
  }

  /**
   * คำนวณ exclusion radius จากขนาด structure
   */
  public static int calculateRadius(StructureData data) {
    int maxDim = Math.max(data.getWidth(), data.getLength());
    return (maxDim / 16) + 1; // อย่างน้อย 1 chunk padding
  }

  /**
   * เคลียร์ทั้งหมด (เมื่อ reload)
   */
  public void clear() {
    occupiedChunks.clear();
  }
}
