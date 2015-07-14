package com.jetbrains.jetpad.vclang.module;

import java.io.File;

public class FileSourceSupplier implements SourceSupplier {
  private final File myDirectory;
  private final ModuleLoader myModuleLoader;

  public FileSourceSupplier(ModuleLoader moduleLoader, File directory) {
    myModuleLoader = moduleLoader;
    myDirectory = directory;
  }

  @Override
  public FileSource getSource(Module module) {
    return new FileSource(myModuleLoader, module, myDirectory == null ? null : module.getFile(myDirectory, ".vc"));
  }
}
