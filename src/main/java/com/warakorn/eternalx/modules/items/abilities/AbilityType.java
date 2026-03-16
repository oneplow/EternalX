package com.warakorn.eternalx.modules.items.abilities;

/**
 * รูปแบบของ Event ที่จะเรียกใช้ Ability
 */
public enum AbilityType {
  ON_HIT,           // ทำงานเมื่อตีศัตรู
  RIGHT_CLICK,      // ทำงานเมื่อคลิกขวาใช้งาน
  PASSIVE,          // ทำงานตลอดเวลา หรือเช็คเป้นระยะ (timer/move)
  ON_KILL,          // ทำงานเมื่อฆ่าศัตรูตาย
  ON_DAMAGE_TAKEN,  // ทำงานเมื่อได้รับความเสียหาย
  SNEAK,            // ทำงานเมื่อกด Shift (Sneak)
  JUMP              // ทำงานเมื่อกระโดด
}
