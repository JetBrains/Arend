package org.arend.frontend.library;

import org.arend.error.ErrorReporter;
import org.arend.library.LibraryConfig;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryHeader;
import org.arend.library.error.LibraryIOError;
import org.arend.module.ModulePath;
import org.arend.typechecking.TypecheckerState;
import org.arend.util.FileUtils;
import org.arend.util.Range;
import org.arend.util.Version;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

public class FileLoadableHeaderLibrary extends FileSourceLibrary {
  private final LibraryConfig myConfig;
  private final Path myHeaderFile;

  public FileLoadableHeaderLibrary(LibraryConfig config, Path headerFile, TypecheckerState typecheckerState) {
    super(config.getName(), null, null, Collections.emptySet(), config.getModules() != null, Collections.emptyList(), Range.unbound(), typecheckerState);
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

    if (myConfig.getBinariesDir() != null) {
      myBinaryBasePath = myHeaderFile.getParent().resolve(myConfig.getBinariesDir());
    }

    myModules = new LinkedHashSet<>();
    if (myConfig.getModules() != null) {
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
        FileUtils.getModules(mySourceBasePath, FileUtils.EXTENSION, myModules, errorReporter);
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

    if (myConfig.getLangVersion() != null) {
      Range<Version> range = Range.parseVersionRange(myConfig.getLangVersion());
      if (range != null) {
        myLanguageVersion = range;
      } else {
        errorReporter.report(new LibraryIOError(myHeaderFile.toString(), "Cannot parse language version: " + myConfig.getLangVersion()));
        return null;
      }
    }

    return new LibraryHeader(myModules, myDependencies, myLanguageVersion);
  }
}
