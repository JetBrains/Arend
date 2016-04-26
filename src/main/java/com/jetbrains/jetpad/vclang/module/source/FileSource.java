package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, FileModuleID module, File file) {
    super(errorReporter, module);
    myFile = file;
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }

  @Override
  public ModuleLoader.Result load() throws IOException {
    setStream(new FileInputStream(myFile));
    return super.load();
  }
}
