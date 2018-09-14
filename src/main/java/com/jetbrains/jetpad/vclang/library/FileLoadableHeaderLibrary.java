package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.error.LibraryIOError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class FileLoadableHeaderLibrary extends FileSourceLibrary {
  private final LibraryConfig myConfig;
  private final Path myHeaderFile;

  public FileLoadableHeaderLibrary(LibraryConfig config, Path headerFile, TypecheckerState typecheckerState) {
    super(config.getName(), null, null, Collections.emptySet(), config.getModules() != null, Collections.emptyList(), typecheckerState);
    myConfig = config;
    myHeaderFile = headerFile;
  }

  public Path getHeaderFile() {
    return myHeaderFile;
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    if (myConfig.getSourcesDir() != null) {
      mySourceBasePath = myHeaderFile.getParent().resolve(myConfig.getSourcesDir());
    }

    if (myConfig.getOutputDir() != null) {
      myBinaryBasePath = myHeaderFile.getParent().resolve(myConfig.getOutputDir());
    }

    if (myConfig.getModules() != null) {
      myModules = new HashSet<>();
      for (String module : myConfig.getModules()) {
        ModulePath modulePath = FileUtils.modulePath(module);
        if (modulePath != null) {
          myModules.add(modulePath);
        } else {
          errorReporter.report(new LibraryIOError(myHeaderFile.toString(), "Illegal module name: " + module));
        }
      }
    } else {
      if (mySourceBasePath != null) {
        myModules = FileUtils.getModules(mySourceBasePath, FileUtils.EXTENSION);
      }
    }

    if (myConfig.getDependencies() != null) {
      myDependencies = new ArrayList<>();
      for (String library : myConfig.getDependencies()) {
        if (FileUtils.isLibraryName(library)) {
          myDependencies.add(new LibraryDependency(library));
        } else {
          errorReporter.report(new LibraryIOError(myHeaderFile.toString(), "Illegal library name: " + library));
        }
      }
    }

    return new LibraryHeader(myModules, myDependencies);
  }
}
