package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileOutputSupplier implements OutputSupplier {
  private final File myDirectory;
  private final List<File> myLibDirs;
  private final ModuleDeserialization myModuleDeserialization;

  public FileOutputSupplier(ModuleDeserialization moduleDeserialization, File directory, List<File> libDirs) {
    myDirectory = directory;
    myLibDirs = libDirs;
    myModuleDeserialization = moduleDeserialization;
  }

  @Override
  public FileOutput getOutput(ResolvedName module) {
    File file = myDirectory == null ? null : FileOperations.getFile(myDirectory, module, FileOperations.SERIALIZED_EXTENSION);
    File directory = myDirectory == null ? null : FileOperations.getFile(myDirectory, module, "");
    List<String> children = directory != null && directory.exists() ?
        FileOperations.getChildren(directory, FileOperations.SERIALIZED_EXTENSION) :
        file != null && file.exists() ? Collections.<String>emptyList() : null;

    return new FileOutput(myModuleDeserialization, module, file, children);
  }

  @Override
  public FileOutput locateOutput(ResolvedName resolvedName) {
    List<String> children = null;
    File file = null;
    for (File dir : myLibDirs) {
      File maybeFile = FileOperations.getFile(dir, resolvedName, FileOperations.SERIALIZED_EXTENSION);
      if (maybeFile.exists()) {
        if (file != null) {
          // TODO: ambigous module
          return new FileOutput(myModuleDeserialization, resolvedName, null, null);
        }
        file = maybeFile;
      }
      File maybeDir = FileOperations.getFile(dir, resolvedName, "");
      if (maybeDir.exists() && maybeDir.isDirectory()) {
        if (children == null)
          children = new ArrayList<>();
        children.addAll(FileOperations.getChildren(maybeDir, FileOperations.SERIALIZED_EXTENSION));
      }
    }

    return new FileOutput(myModuleDeserialization, resolvedName, file, children != null ? children : file != null ? Collections.<String>emptyList() : null);
  }
}
