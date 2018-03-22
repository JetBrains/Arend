package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * A library which is used to load and persist prelude from and to a file.
 */
public class PreludeFileLibrary extends PreludeTypecheckingLibrary {
  private final Path myBinaryPath;

  /**
   * Creates a new {@code PreludeFileLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  public PreludeFileLibrary(Path binaryPath, TypecheckerState typecheckerState) {
    super(typecheckerState);
    myBinaryPath = binaryPath;
  }

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      return null;
    }
    return new FileRawSource(PreludeResourceSource.BASE_PATH, Prelude.MODULE_PATH);
  }

  @Nullable
  @Override
  public BinarySource getBinarySource(ModulePath modulePath) {
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
