package com.jetbrains.jetpad.vclang.util;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  private static Path baseFile(Path root, ModulePath modulePath) {
    return root.resolve(Paths.get("", modulePath.toArray()));
  }

  public static Path sourceFile(Path root, ModulePath modulePath) {
    Path base = baseFile(root, modulePath);
    return base.resolveSibling(base.getFileName() + FileUtils.EXTENSION);
  }

  public static Path cacheFile(Path root, ModulePath modulePath) {
    Path base = baseFile(root, modulePath);
    return base.resolveSibling(base.getFileName() + FileUtils.SERIALIZED_EXTENSION);
  }
}
