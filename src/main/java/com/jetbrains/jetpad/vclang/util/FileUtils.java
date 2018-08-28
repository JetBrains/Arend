package com.jetbrains.jetpad.vclang.util;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FileUtils {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";
  public static final String LIBRARY_EXTENSION = ".vcl";

  private static Path baseFile(Path root, ModulePath modulePath) {
    return root.resolve(Paths.get("", modulePath.toArray()));
  }

  public static Path sourceFile(Path root, ModulePath modulePath) {
    Path base = baseFile(root, modulePath);
    return base.resolveSibling(base.getFileName() + FileUtils.EXTENSION);
  }

  public static Path binaryFile(Path root, ModulePath modulePath) {
    Path base = baseFile(root, modulePath);
    return base.resolveSibling(base.getFileName() + FileUtils.SERIALIZED_EXTENSION);
  }

  private static final String MODULE_NAME_START_SYMBOL_REGEX = "a-zA-Z_"; // "~!@#$%^&*\\-+=<>?/|:;\\[\\]a-zA-Z_"
  private static final String MODULE_NAME_REGEX = "[" + MODULE_NAME_START_SYMBOL_REGEX + "][" + MODULE_NAME_START_SYMBOL_REGEX + "0-9']*";
  private static final String LIBRARY_NAME_REGEX = "[" + MODULE_NAME_START_SYMBOL_REGEX + "][" + MODULE_NAME_START_SYMBOL_REGEX + "0-9\\-.']*";

  public static String libraryName(String fileName) {
    if (!fileName.endsWith(LIBRARY_EXTENSION)) {
      return null;
    }

    String libName = fileName.substring(0, fileName.length() - LIBRARY_EXTENSION.length());
    return libName.matches(LIBRARY_NAME_REGEX) ? libName : null;
  }

  public static boolean isLibraryName(String name) {
    return name.matches(LIBRARY_NAME_REGEX);
  }

  public static boolean isModuleName(String name) {
    return name.matches(MODULE_NAME_REGEX);
  }

  public static ModulePath modulePath(Path path, String ext) {
    assert !path.isAbsolute();

    if (ext != null) {
      String fileName = path.getFileName().toString();
      if (!fileName.endsWith(ext) || fileName.length() == ext.length()) {
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

  public static ModulePath modulePath(String path) {
    ModulePath modulePath = ModulePath.fromString(path);
    for (String name : modulePath.toList()) {
      if (!name.matches(MODULE_NAME_REGEX)) {
        return null;
      }
    }
    return modulePath;
  }

  public static Path getCurrentDirectory() {
    return Paths.get(System.getProperty("user.dir"));
  }

  public static void printIllegalModuleName(String module) {
    System.err.println("[ERROR] " + module + " is an illegal module path");
  }

  public static Set<ModulePath> getModules(Path path, String ext) {
    Set<ModulePath> modules = new LinkedHashSet<>();
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.getFileName().toString().endsWith(ext)) {
            file = path.relativize(file);
            ModulePath modulePath = FileUtils.modulePath(file, ext);
            if (modulePath == null) {
              printIllegalModuleName(file.toString());
            } else {
              modules.add(modulePath);
            }
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) {
          e.printStackTrace();
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
    return modules;
  }
}
