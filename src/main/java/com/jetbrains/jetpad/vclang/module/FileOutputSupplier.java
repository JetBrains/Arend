package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;

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
  public FileOutput getOutput(Module module) {
    return new FileOutput(myModuleDeserialization, myDirectory == null ? null : module.getFile(myDirectory, ".vcc"));
  }

  @Override
  public FileOutput locateOutput(Module module) {
    for (File dir : myLibDirs) {
      File file = module.getFile(dir, ".vcc");
      if (file != null && file.exists()) {
        return new FileOutput(myModuleDeserialization, file);
      }
    }

    return new FileOutput(myModuleDeserialization, null);
  }
}
