package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.module.Namespace;
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
  public FileOutput getOutput(Namespace module) {
    return new FileOutput(myModuleDeserialization, module, myDirectory == null ? null : FileOperations.getFile(myDirectory, module, ".vcc"));
  }

  @Override
  public FileOutput locateOutput(Namespace module) {
    for (File dir : myLibDirs) {
      File file = FileOperations.getFile(dir, module, ".vcc");
      if (file.exists()) {
        return new FileOutput(myModuleDeserialization, module, file);
      }
    }

    return new FileOutput(myModuleDeserialization, module, null);
  }
}
