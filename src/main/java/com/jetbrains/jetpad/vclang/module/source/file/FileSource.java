package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileSource extends ParseSource {
  private final File myFile;

  public FileSource(ModuleSourceId sourceId, File file, ErrorReporter errorReporter) throws FileNotFoundException {
    super(sourceId, new FileInputStream(file), errorReporter);
    myFile = file;
  }

  @Override
  public long lastModified() {
    return myFile.lastModified();
  }
}
