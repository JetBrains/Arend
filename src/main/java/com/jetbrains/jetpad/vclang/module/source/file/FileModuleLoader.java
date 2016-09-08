package com.jetbrains.jetpad.vclang.module.source.file;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ReportingModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;

import java.io.File;
import java.io.FileNotFoundException;

public abstract class FileModuleLoader extends ReportingModuleLoader<FileModuleSourceId> {
  private final File myDirectory;
  private final ErrorReporter myErrorReporter;

  public FileModuleLoader(File directory, ErrorReporter errorReporter) {
    super(errorReporter);
    myDirectory = directory;
    myErrorReporter = errorReporter;
  }

  @Override
  public Source getSource(FileModuleSourceId moduleSourceId) {
    File file = FileOperations.getFile(myDirectory, moduleSourceId.getModulePath(), FileOperations.EXTENSION);
    if (file.exists()) {
      try {
        return new FileSource(moduleSourceId, file, myErrorReporter);
      } catch (FileNotFoundException ignored) {
      }
    }
    return null;
  }

  @Override
  public FileModuleSourceId locateModule(ModulePath modulePath) {
    return new FileModuleSourceId(modulePath);
  }
}
