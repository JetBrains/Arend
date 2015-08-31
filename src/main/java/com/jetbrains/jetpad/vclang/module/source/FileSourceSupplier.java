package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

import java.io.File;

public class FileSourceSupplier implements SourceSupplier {
  private final File myDirectory;
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;

  public FileSourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter, File directory) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    myDirectory = directory;
  }

  @Override
  public FileSource getSource(Namespace module) {
    return new FileSource(myModuleLoader, myErrorReporter, module, myDirectory);
  }
}
