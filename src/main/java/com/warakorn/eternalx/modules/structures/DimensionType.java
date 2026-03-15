package com.warakorn.eternalx.modules.structures;

import org.bukkit.World;

public enum DimensionType {
  OVERWORLD,
  NETHER,
  THE_END,
  ANY;

  public static DimensionType fromWorld(World world) {
    return switch (world.getEnvironment()) {
      case NORMAL -> OVERWORLD;
      case NETHER -> NETHER;
      case THE_END -> THE_END;
      default -> OVERWORLD;
    };
  }

  public boolean matches(World world) {
    if (this == ANY) return true;
    return this == fromWorld(world);
  }
}
