package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileOperations {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  public static File getFile(File dir, Namespace namespace) {
    return namespace == null || namespace.getParent() == null ? dir : new File(getFile(dir, namespace.getParent()), namespace.getName());
  }

  public static File getFile(File dir, ResolvedName resolvedName, String ext) {
    return new File(getFile(dir, resolvedName.parent), resolvedName.name.name + ext);
  }

  public static String getExtFileName(File file, String extension) {
    String name = file.getName();
    if (name.endsWith(extension)) {
      return name.substring(0, name.length() - extension.length());
    } else {
      return null;
    }
  }

  public static List<String> getChildren(File directory, String extension) {
    List<String> result = new ArrayList<>();
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          result.add(file.getName());
        } else if (file.isFile()) {
          String name = FileOperations.getExtFileName(file, extension);
          if (name != null) {
            result.add(name);
          }
        }
      }
    }
    return result;
  }
}
