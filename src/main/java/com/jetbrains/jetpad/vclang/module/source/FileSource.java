package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

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
  public ModuleLoadingResult load(Namespace namespace) throws IOException {
    boolean ok = false;
    if (myDirectory != null) {
      File[] files = myDirectory.listFiles();
      if (files != null) {
        ok = true;
        for (File file : files) {
          if (file.isDirectory()) {
            namespace.getChild(new Utils.Name(file.getName()));
          } else if (file.isFile()) {
            String name = FileOperations.getVcFileName(file);
            if (name != null) {
              namespace.getChild(new Utils.Name(name));
            }
          }
        }
      }
    }

    if (myFile != null && myFile.exists()) {
      setStream(new FileInputStream(myFile));
      return super.load(namespace);
    } else {
      return ok ? new ModuleLoadingResult(namespace, null, true, 0) : null;
    }
  }
}
