package org.arend.util;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.library.error.LibraryIOError;
import org.arend.module.error.ExceptionError;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtils {
  public static final String EXTENSION = ".ard";
  public static final String SERIALIZED_EXTENSION = ".arc";
  public static final String LIBRARY_CONFIG_FILE = "arend.yaml";

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

  public static GeneralError illegalModuleName(String module) {
    return new GeneralError(GeneralError.Level.ERROR, module + " is an illegal module path");
  }

  public static void getModules(Path path, String ext, Collection<ModulePath> modules, ErrorReporter errorReporter) {
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.getFileName().toString().endsWith(ext)) {
            file = path.relativize(file);
            ModulePath modulePath = FileUtils.modulePath(file, ext);
            if (modulePath == null) {
              errorReporter.report(illegalModuleName(file.toString()));
            } else {
              modules.add(modulePath);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (NoSuchFileException e) {
      errorReporter.report(new LibraryIOError(e.getFile(), "No such file"));
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, "processing of directory " + path));
    }
  }
}
