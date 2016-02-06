package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, FileModuleID module, File file) {
    super(moduleLoader, errorReporter, module);
    myFile = file;
  }

  @Override
  public boolean isAvailable() {
    return myFile.exists();
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public ModuleLoader.Result load() throws IOException {
    if (!isAvailable())
      return null;

    setStream(new FileInputStream(myFile));
    return super.load();
  }
}
