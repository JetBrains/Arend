package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;

import java.io.*;

public class FileSourceSupplier implements SourceSupplier {
  private final File myDirectory;
  private final ErrorReporter myErrorReporter;

  public FileSourceSupplier(ErrorReporter errorReporter, File directory) {
    myErrorReporter = errorReporter;
    myDirectory = directory;
  }

  @Override
  public Source getSource(ModuleID module) {
    if (!(module instanceof FileModuleID))
      return null;
    File file = FileOperations.getFile(myDirectory, module.getModulePath(), FileOperations.EXTENSION);
    return file.exists() ? new FileSource(myErrorReporter, (FileModuleID) module, file) : null;
  }

  @Override
  public FileModuleID locateModule(ModulePath modulePath) {
    return new FileModuleID(modulePath);
  }
}
