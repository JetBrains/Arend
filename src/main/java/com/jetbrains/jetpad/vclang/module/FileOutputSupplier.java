package com.jetbrains.jetpad.vclang.module;

import java.io.File;
import java.util.List;

public class FileOutputSupplier implements OutputSupplier {
  private final File myDirectory;
  private final List<File> myLibDirs;

  public FileOutputSupplier(File directory, List<File> libDirs) {
    myDirectory = directory;
    myLibDirs = libDirs;
  }

  @Override
  public FileOutput getOutput(Module module) {
    return new FileOutput(myDirectory == null ? null : module.getFile(myDirectory, ".vcc"));
  }

  @Override
  public FileOutput locateOutput(Module module) {
    for (File dir : myLibDirs) {
      File file = module.getFile(dir, ".vcc");
      if (file != null && file.exists()) {
        return new FileOutput(file);
      }
    }

    return new FileOutput(null);
  }
}
