package com.warakorn.eternalx.modules.structures;

public enum BiomeMatchMode {
  ANY,        // ไม่สนใจ biome เลย
  WHITELIST,  // อนุญาตเฉพาะ biome ที่ระบุ
  BLACKLIST,  // ห้าม biome ที่ระบุ
  STRICT      // ต้องผ่านทั้ง whitelist และ blacklist
}
