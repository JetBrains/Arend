package com.jetbrains.jetpad.vclang.util;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

  private static final String MODULE_NAME_START_SYMBOL_REGEX = "a-zA-Z_"; // "~!@#$%^&*\\-+=<>?/|:;\\[\\]a-zA-Z_"
  private static final String MODULE_NAME_REGEX = "[" + MODULE_NAME_START_SYMBOL_REGEX + "][" + MODULE_NAME_START_SYMBOL_REGEX + "0-9']";

  public static ModulePath modulePath(Path path, String ext) {
    assert !path.isAbsolute();

    if (ext != null) {
      String fileName = path.getFileName().toString();
      if (!fileName.endsWith(ext)) {
        return null;
      }
      path = path.resolveSibling(fileName.substring(0, fileName.length() - ext.length()));
    }

    List<String> names = new ArrayList<>();
    for (Path elem : path) {
      String name = elem.toString();
      if (!name.matches(MODULE_NAME_REGEX)) {
        return null;
      }
      names.add(name);
    }

    return new ModulePath(names);
  }
}
