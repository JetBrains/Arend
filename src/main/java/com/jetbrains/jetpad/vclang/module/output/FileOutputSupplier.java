package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.File;
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
  public FileOutput getOutput(ResolvedName resolvedName) {
    return new FileOutput(myModuleDeserialization, resolvedName, myDirectory == null ? null : FileOperations.getFile(myDirectory, resolvedName, ".vcc"));
  }

  @Override
  public FileOutput locateOutput(ResolvedName resolvedName) {
    for (File dir : myLibDirs) {
      File file = FileOperations.getFile(dir, resolvedName, ".vcc");
      if (file.exists()) {
        return new FileOutput(myModuleDeserialization, resolvedName, file);
      }
    }

    return new FileOutput(myModuleDeserialization, resolvedName, null);
  }
}
