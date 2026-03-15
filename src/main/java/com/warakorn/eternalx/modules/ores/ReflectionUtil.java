package com.warakorn.eternalx.modules.ores;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ReflectionUtil {

  private static Field featuresField;
  private static Method generationSettingsMethod; // เก็บ Method ที่ใช้ดึง Settings

  static {
    // 1. หา field 'features' ใน BiomeGenerationSettings
    try {
      for (Field field : BiomeGenerationSettings.class.getDeclaredFields()) {
        if (field.getType() == List.class) {
          field.setAccessible(true);
          featuresField = field;
          break;
        }
      }
      if (featuresField == null) {
        try {
          featuresField = BiomeGenerationSettings.class.getDeclaredField("features");
          featuresField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 2. หา method getGenerationSettings() ใน Biome
    try {
      // วนหา method ที่ return BiomeGenerationSettings และไม่มี parameter
      for (Method method : Biome.class.getMethods()) {
        if (method.getReturnType() == BiomeGenerationSettings.class && method.getParameterCount() == 0) {
          method.setAccessible(true);
          generationSettingsMethod = method;
          break;
        }
      }
      // ถ้าไม่เจอ public ให้หา declared (เผื่อ private/protected)
      if (generationSettingsMethod == null) {
        for (Method method : Biome.class.getDeclaredMethods()) {
          if (method.getReturnType() == BiomeGenerationSettings.class && method.getParameterCount() == 0) {
            method.setAccessible(true);
            generationSettingsMethod = method;
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ✅ ฟังก์ชันใหม่: ดึง BiomeGenerationSettings ด้วย Reflection
  public static BiomeGenerationSettings getGenerationSettings(Biome biome) {
    try {
      if (generationSettingsMethod != null) {
        return (BiomeGenerationSettings) generationSettingsMethod.invoke(biome);
      }

      // Fallback: ถ้าหา method ไม่เจอ ลองหา field ตรงๆ
      for (Field field : Biome.class.getDeclaredFields()) {
        if (field.getType() == BiomeGenerationSettings.class) {
          field.setAccessible(true);
          return (BiomeGenerationSettings) field.get(biome);
        }
      }

      throw new RuntimeException("Could not find BiomeGenerationSettings in Biome class");
    } catch (Exception e) {
      throw new RuntimeException("Failed to get generation settings", e);
    }
  }

  public static Object getNMSLevel(World world) {
    try {
      Method getHandle = world.getClass().getMethod("getHandle");
      return getHandle.invoke(world);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get NMS Level handle", e);
    }
  }

  public static RegistryAccess getRegistryAccessFromLevel(Object nmsLevel) {
    try {
      Class<?> clazz = nmsLevel.getClass();
      while (clazz != null) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (RegistryAccess.class.isAssignableFrom(method.getReturnType())
            && method.getParameterCount() == 0) {
            method.setAccessible(true);
            return (RegistryAccess) method.invoke(nmsLevel);
          }
        }
        clazz = clazz.getSuperclass();
      }
      throw new RuntimeException("Method returning RegistryAccess not found");
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke registryAccess()", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Object> getFeaturesList(BiomeGenerationSettings settings) {
    try {
      if (featuresField != null) {
        return (List<Object>) featuresField.get(settings);
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void setFeaturesList(BiomeGenerationSettings settings, List<Object> newList) {
    try {
      if (featuresField != null) {
        featuresField.set(settings, newList);
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
