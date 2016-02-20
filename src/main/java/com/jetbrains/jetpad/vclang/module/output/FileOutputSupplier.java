package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;

import java.io.File;
import java.util.*;

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
  public Output getOutput(ModuleID module) {
    if (!(module instanceof FileModuleID)) {
      return null;
    }
    for (File dir : myLibDirs) {
      File file = FileOperations.getFile(dir, module.getModulePath(), FileOperations.SERIALIZED_EXTENSION);
      if (file.exists()) {
        return new FileOutput(myModuleDeserialization, (FileModuleID) module, file, true);
      }
    }
    File file = FileOperations.getFile(myDirectory, module.getModulePath(), FileOperations.SERIALIZED_EXTENSION);
    return new FileOutput(myModuleDeserialization, (FileModuleID) module, file, false);
  }

  @Override
  public FileModuleID locateModule(ModulePath modulePath) {
    return new FileModuleID(modulePath);
  }
}
