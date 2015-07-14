package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.serialization.ModuleSerialization;

import java.io.File;
import java.util.List;

public class FileOutputSupplier implements OutputSupplier {
  private final File myDirectory;
  private final List<File> myLibDirs;
  private final ModuleSerialization myModuleSerialization;

  public FileOutputSupplier(ModuleSerialization moduleSerialization, File directory, List<File> libDirs) {
    myDirectory = directory;
    myLibDirs = libDirs;
    myModuleSerialization = moduleSerialization;
  }

  @Override
  public FileOutput getOutput(Module module) {
    return new FileOutput(myModuleSerialization, myDirectory == null ? null : module.getFile(myDirectory, ".vcc"));
  }

  @Override
  public FileOutput locateOutput(Module module) {
    for (File dir : myLibDirs) {
      File file = module.getFile(dir, ".vcc");
      if (file != null && file.exists()) {
        return new FileOutput(myModuleSerialization, file);
      }
    }

    return new FileOutput(myModuleSerialization, null);
  }
}
