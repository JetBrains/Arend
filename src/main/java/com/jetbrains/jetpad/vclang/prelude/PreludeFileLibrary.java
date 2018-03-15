package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * A library which is used to load and persist prelude from and to a file.
 */
public class PreludeFileLibrary extends PreludeLibrary {
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

  @Override
  public boolean load(LibraryManager libraryManager) {
    synchronized (PreludeLibrary.class) {
      if (getPreludeScope() == null) {
        return super.load(libraryManager);
      }
    }

    Prelude.fillInTypecheckerState(getTypecheckerState());
    setLoaded();
    return true;
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
  public Collection<? extends ModulePath> getUpdatedModules() {
    return Collections.singleton(Prelude.MODULE_PATH);
  }

  @Override
  public boolean needsTypechecking() {
    return true;
  }

  @Override
  public boolean supportsPersisting() {
    return myBinaryPath != null;
  }

  @Override
  public boolean typecheck(Typechecking typechecking) {
    if (super.typecheck(typechecking)) {
      synchronized (PreludeLibrary.class) {
        if (Prelude.INTERVAL == null) {
          Prelude.initialize(getPreludeScope(), getTypecheckerState());
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
