package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.source.BinarySource;
import com.jetbrains.jetpad.vclang.source.GZIPStreamBinarySource;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nullable;

/**
 * A library which is used to load prelude from a resource.
 */
public class PreludeResourceLibrary extends PreludeLibrary {
  /**
   * Creates a new {@code PreludeResourceLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  public PreludeResourceLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public boolean load(LibraryManager libraryManager) {
    synchronized (PreludeLibrary.class) {
      if (getPreludeScope() == null) {
        if (super.load(libraryManager)) {
          Prelude.initialize(getPreludeScope(), getTypecheckerState());
          return true;
        } else {
          return false;
        }
      }
    }

    Prelude.fillInTypecheckerState(getTypecheckerState());
    setLoaded();
    return true;
  }

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    return null;
  }

  @Nullable
  @Override
  public BinarySource getBinarySource(ModulePath modulePath) {
    return new GZIPStreamBinarySource(new PreludeResourceSource());
  }

  @Override
  public boolean hasRawSources() {
    return false;
  }

  @Nullable
  @Override
  public ReferableConverter getReferableConverter() {
    return null;
  }

  @Override
  public boolean supportsPersisting() {
    return false;
  }
}
