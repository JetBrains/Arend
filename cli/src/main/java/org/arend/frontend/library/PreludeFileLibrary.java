package org.arend.frontend.library;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.source.FileRawSource;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeResourceSource;
import org.arend.prelude.PreludeTypecheckingLibrary;
import org.arend.source.*;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A library which is used to load and persist prelude from and to a file.
 */
public class PreludeFileLibrary extends PreludeTypecheckingLibrary {
  private final Path myBinaryPath;

  /**
   * Creates a new {@code PreludeFileLibrary}
   */
  public PreludeFileLibrary(Path binaryPath) {
    myBinaryPath = binaryPath;
  }

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      return null;
    }

    Path preludePath = PreludeResourceSource.BASE_PATH;
    String arendPath = System.getenv("AREND_PATH");
    if (arendPath != null) {
      preludePath = Paths.get(arendPath).resolve(preludePath);
    }
    return new FileRawSource(preludePath, Prelude.MODULE_PATH, false);
  }

  @Nullable
  @Override
  public PersistableBinarySource getPersistableBinarySource(ModulePath modulePath) {
    if (myBinaryPath == null || !modulePath.equals(Prelude.MODULE_PATH)) {
      return null;
    }
    return new GZIPStreamBinarySource(new FileBinarySource(myBinaryPath.resolve(PreludeResourceSource.BASE_PATH), Prelude.MODULE_PATH));
  }

  @Override
  public boolean supportsPersisting() {
    return myBinaryPath != null;
  }
}
