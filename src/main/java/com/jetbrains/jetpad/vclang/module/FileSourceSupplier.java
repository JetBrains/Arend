package com.jetbrains.jetpad.vclang.module;

import java.io.File;

public class FileSourceSupplier implements SourceSupplier {
  private final File myDirectory;

  public FileSourceSupplier(File directory) {
    myDirectory = directory;
  }

  @Override
  public FileSource getSource(Module module) {
    return new FileSource(module, myDirectory == null ? null : module.getFile(myDirectory, ".vc"));
  }
}
