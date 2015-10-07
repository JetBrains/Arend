package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.*;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;
  private final File myDirectory;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, Namespace module, File baseDirectory) {
    super(moduleLoader, errorReporter, module);
    myFile = FileOperations.getFile(baseDirectory, module, FileOperations.EXTENSION);
    myDirectory = FileOperations.getFile(baseDirectory, module, "");
  }

  @Override
  public boolean isAvailable() {
    return myFile != null && myFile.exists() || myDirectory != null && myDirectory.exists() && myDirectory.isDirectory();
  }

  @Override
  public long lastModified() {
    if (myFile != null && myFile.exists()) {
      return myFile.lastModified();
    } else {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public ModuleLoadingResult load() throws IOException {
    boolean ok = false;
    if (myDirectory != null) {
      File[] files = myDirectory.listFiles();
      if (files != null) {
        ok = true;
        for (File file : files) {
          if (file.isDirectory()) {
            getModule().getChild(new Name(file.getName()));
          } else if (file.isFile()) {
            String name = FileOperations.getVcFileName(file);
            if (name != null) {
              getModule().getChild(new Name(name));
            }
          }
        }
      }
    }

    if (myFile != null && myFile.exists()) {
      setStream(new FileInputStream(myFile));
      return super.load();
    } else {
      return ok ? new ModuleLoadingResult(getModule(), new DefinitionPair(getModule(), null, null), true, 0) : null;
    }
  }
}
