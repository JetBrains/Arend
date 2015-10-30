package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
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
  public ModuleLoadingResult load() throws IOException {
    Namespace namespace = null;

    if (myDirectory != null) {
      File[] files = myDirectory.listFiles();
      if (files != null) {
        ResolvedName resolvedName = getModule();
        namespace = resolvedName.parent.getChild(resolvedName.name);

        for (File file : files) {
          if (file.isDirectory()) {
            namespace.getChild(new Name(file.getName()));
          } else if (file.isFile()) {
            String name = FileOperations.getVcFileName(file);
            if (name != null) {
              namespace.getChild(new Name(name));
            }
          }
        }
      }
    }

    if (myFile != null && myFile.exists()) {
      setStream(new FileInputStream(myFile));
      return super.load();
    } else {
      return namespace != null ? new ModuleLoadingResult(new NamespaceMember(namespace, null, null), true, 0) : null;
    }
  }
}
