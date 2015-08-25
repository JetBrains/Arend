package com.jetbrains.jetpad.vclang.module;

import java.io.File;

public class DirectorySourceSupplier implements SourceSupplier {
  private final File myDirectory;

  public DirectorySourceSupplier(File directory) {
    myDirectory = directory;
  }

  @Override
  public Source getSource(Module module) {
    return new DirectorySource(myDirectory == null ? null : module.getFile(myDirectory, ""));
  }
}
