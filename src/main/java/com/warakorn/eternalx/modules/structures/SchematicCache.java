package com.warakorn.eternalx.modules.structures;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.warakorn.eternalx.EternalX;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy-loading cache สำหรับ schematic files
 * ใช้ SoftReference → GC จะลบเมื่อ memory ใกล้เต็ม
 * โหลดใหม่อัตโนมัติเมื่อต้องใช้
 */
public class SchematicCache {
  private final EternalX plugin;
  private final Map<String, SoftReference<Clipboard>> cache = new ConcurrentHashMap<>();
  private final Map<String, File> fileMap = new ConcurrentHashMap<>();

  public SchematicCache(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * ลงทะเบียน schematic file สำหรับ structure id
   */
  public void register(String structureId, File schemFile) {
    fileMap.put(structureId, schemFile);
  }

  /**
   * ดึง Clipboard — โหลดจาก cache หรือจากไฟล์ถ้ายังไม่มี/ถูก GC ลบไปแล้ว
   */
  public Clipboard getClipboard(String structureId) {
    SoftReference<Clipboard> ref = cache.get(structureId);
    Clipboard clipboard = (ref != null) ? ref.get() : null;

    if (clipboard != null) {
      return clipboard;
    }

    // Cache miss — โหลดจากไฟล์
    File file = fileMap.get(structureId);
    if (file == null || !file.exists()) {
      plugin.getLogger().warning("Schematic file not found for: " + structureId);
      return null;
    }

    clipboard = loadFromFile(file);
    if (clipboard != null) {
      cache.put(structureId, new SoftReference<>(clipboard));
      plugin.debugLog("Loaded schematic (lazy): " + structureId);
    }
    return clipboard;
  }

  /**
   * โหลดและ cache ทันที (ใช้ตอน startup เพื่ออ่าน dimensions)
   */
  public Clipboard loadAndCache(String structureId, File schemFile) {
    register(structureId, schemFile);
    Clipboard clipboard = loadFromFile(schemFile);
    if (clipboard != null) {
      cache.put(structureId, new SoftReference<>(clipboard));
    }
    return clipboard;
  }

  private Clipboard loadFromFile(File file) {
    ClipboardFormat format = ClipboardFormats.findByFile(file);
    if (format == null) return null;
    try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
      return reader.read();
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to load schematic: " + file.getName());
      return null;
    }
  }

  /**
   * ลบ cache ของ structure เฉพาะ
   */
  public void invalidate(String structureId) {
    cache.remove(structureId);
  }

  /**
   * เคลียร์ cache ทั้งหมด (เมื่อ reload)
   */
  public void clear() {
    cache.clear();
    fileMap.clear();
  }

  /**
   * จำนวน schematics ที่ยังอยู่ใน memory
   */
  public int getCachedCount() {
    int count = 0;
    for (SoftReference<Clipboard> ref : cache.values()) {
      if (ref.get() != null) count++;
    }
    return count;
  }

  /**
   * จำนวน schematics ที่ลงทะเบียนทั้งหมด
   */
  public int getRegisteredCount() {
    return fileMap.size();
  }
}
