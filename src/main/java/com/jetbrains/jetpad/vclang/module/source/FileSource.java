package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;
  private final File myDirectory;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ResolvedName module, File baseDirectory) {
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
  public boolean isContainer() {
    return myFile == null || !myFile.exists();
  }

  @Override
  public ModuleLoadingResult load(boolean childrenOnly) throws IOException {
    Namespace namespace = null;

    if (myDirectory != null && myDirectory.isDirectory()) {
      namespace = getModule().parent.getChild(getModule().name);
      for (String childName : FileOperations.getChildren(myDirectory, FileOperations.EXTENSION)) {
        namespace.getChild(new Name(childName));
      }
    }

    if (!childrenOnly && myFile != null && myFile.exists()) {
      setStream(new FileInputStream(myFile));
      return super.load(false);
    } else {
      return namespace != null ? new ModuleLoadingResult(getModule().toNamespaceMember(), true, 0) : null;
    }
  }
}
