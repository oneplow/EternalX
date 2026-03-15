package com.warakorn.eternalx.modules.structures.engine;

import com.warakorn.eternalx.modules.structures.StructureData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class StructurePlacementEngine {

  private StructurePlacementEngine() {
  }

  public static boolean shouldSpawnInChunk(long worldSeed, int chunkX, int chunkZ, String structureId, int spacing) {
    int offset = spacing / 3;
    long typeSeed = worldSeed + structureId.hashCode() * 7919L;

    int gridX = Math.floorDiv(chunkX + offset, spacing);
    int gridZ = Math.floorDiv(chunkZ + offset, spacing);

    for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
      for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {

        int baseX = gx * spacing;
        int baseZ = gz * spacing;

        if (gz % 2 != 0) {
          baseX += spacing / 2;
        }

        Random cellRand = new Random(typeSeed ^ (((long) baseX << 32) | (baseZ & 0xFFFFFFFFL)));

        int ox = offset > 0 ? cellRand.nextInt(offset * 2 + 1) - offset : 0;
        int oz = offset > 0 ? cellRand.nextInt(offset * 2 + 1) - offset : 0;

        if (chunkX == baseX + ox && chunkZ == baseZ + oz) {
          return true;
        }
      }
    }
    return false;
  }

  public static Location findPlacementLocation(Chunk chunk, StructureData data, long seed, int rotation) {
    Random rand = new Random(seed + chunk.getX() * 34187312L + chunk.getZ() * 1328979L);
    World world = chunk.getWorld();

    for (int i = 0; i < 10; i++) {
      int x = (chunk.getX() << 4) + rand.nextInt(16);
      int z = (chunk.getZ() << 4) + rand.nextInt(16);

      int width = (rotation == 90 || rotation == 270) ? data.getLength() : data.getWidth();
      int length = (rotation == 90 || rotation == 270) ? data.getWidth() : data.getLength();

      int y = switch (data.getPlacementType()) {
        case SURFACE -> determineSurfaceY(world, x, z, width, length);
        case UNDERGROUND -> determineUndergroundY(world, x, z, rand);
        case CAVE -> determineCaveY(world, x, z, width, length, data.getHeight());
        case UNDERWATER -> determineUnderwaterY(world, x, z);
        case OCEAN_FLOOR -> determineOceanFloorY(world, x, z);
        case FLOATING -> determineFloatingY(world, x, z, rand);
        case SKY -> determineSkyY(world, x, z, rand);
      };

      if (y != -1) {
        return new Location(world, x, y, z);
      }
    }
    return null;
  }

  private static int determineSurfaceY(World world, int cx, int cz, int width, int length) {
    List<Integer> heights = new ArrayList<>();
    int hw = width / 2;
    int hl = length / 2;

    addHeight(world, heights, cx, cz);
    addHeight(world, heights, cx - hw, cz - hl);
    addHeight(world, heights, cx + hw, cz - hl);
    addHeight(world, heights, cx - hw, cz + hl);
    addHeight(world, heights, cx + hw, cz + hl);

    if (heights.size() < 3) return -1;

    Collections.sort(heights);
    if (heights.get(heights.size() - 1) - heights.get(0) > 6) {
      return -1;
    }

    return heights.stream().mapToInt(Integer::intValue).sum() / heights.size();
  }

  private static int determineUndergroundY(World world, int x, int z, Random rand) {
    int maxY = 50;
    int minY = 10;

    for (int attempt = 0; attempt < 5; attempt++) {
      int y = minY + rand.nextInt(maxY - minY);

      boolean hasSpace = true;
      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          for (int dy = 0; dy < 3; dy++) {
            if (world.getBlockAt(x + dx, y + dy, z + dz).getType().isSolid()) {
              hasSpace = false;
              break;
            }
          }
        }
      }

      if (hasSpace) return y;
    }
    return -1;
  }

  // ✅ Fix #6: Optimized cave detection — sample center + corners แทน brute-force ทุก block
  private static int determineCaveY(World world, int x, int z, int width, int length, int height) {
    int hw = width / 2;
    int hl = length / 2;

    // สุ่มตรวจจุดสำคัญ 5 จุด แทนทุก block
    int[][] samplePoints = {
      {x, z},              // center
      {x - hw, z - hl},    // corners
      {x + hw, z - hl},
      {x - hw, z + hl},
      {x + hw, z + hl}
    };

    for (int y = 60; y > 10; y--) {
      // ตรวจว่ามีพื้นแข็งรองรับ
      if (!world.getBlockAt(x, y - 1, z).getType().isSolid()) continue;

      boolean isOpenCave = true;
      for (int[] pt : samplePoints) {
        for (int dy = 0; dy < height; dy++) {
          if (world.getBlockAt(pt[0], y + dy, pt[1]).getType().isSolid()) {
            isOpenCave = false;
            break;
          }
        }
        if (!isOpenCave) break;
      }

      if (isOpenCave) return y;
    }
    return -1;
  }

  private static int determineUnderwaterY(World world, int x, int z) {
    int seaLevel = world.getSeaLevel();
    int surfaceY = world.getHighestBlockYAt(x, z);

    if (surfaceY >= seaLevel) return -1;

    for (int y = seaLevel; y > world.getMinHeight(); y--) {
      Material m = world.getBlockAt(x, y, z).getType();
      if (m.isSolid()) {
        return y + 1;
      }
    }
    return -1;
  }

  private static int determineOceanFloorY(World world, int x, int z) {
    int surfaceY = world.getHighestBlockYAt(x, z);

    if (surfaceY > 45) return -1;

    return surfaceY;
  }

  private static int determineFloatingY(World world, int x, int z, Random rand) {
    int groundY = getSolidGroundY(world, x, z);
    if (groundY == -1) return -1;

    return groundY + 20 + rand.nextInt(30);
  }

  private static int determineSkyY(World world, int x, int z, Random rand) {
    return 120 + rand.nextInt(50);
  }

  private static void addHeight(World world, List<Integer> heights, int x, int z) {
    int y = getSolidGroundY(world, x, z);
    if (y != -1) heights.add(y);
  }

  private static int getSolidGroundY(World world, int x, int z) {
    for (int y = world.getHighestBlockYAt(x, z); y > world.getMinHeight(); y--) {
      Material m = world.getBlockAt(x, y, z).getType();
      if (m.isAir()) continue;
      if (m.name().contains("LEAVES") || m.name().contains("LOG")) continue;
      if (m == Material.SHORT_GRASS || m == Material.TALL_GRASS || m == Material.FERN || m == Material.SNOW || m == Material.VINE)
        continue;
      if (m.isSolid()) return y + 1;
    }
    return -1;
  }
}
