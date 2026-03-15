package com.warakorn.eternalx.modules.structures;

public enum RotationMode {
  NONE,           // ไม่หมุนเลย
  RANDOM,         // สุ่ม 0°, 90°, 180°, 270°
  CARDINAL_ONLY,  // หมุนได้แค่ 0° และ 180°
  CUSTOM          // กำหนดมุมเองใน config
}
