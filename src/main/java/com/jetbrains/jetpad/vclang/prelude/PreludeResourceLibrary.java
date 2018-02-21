package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.PersistableSource;
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

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    return null;
  }

  @Nullable
  @Override
  public PersistableSource getBinarySource(ModulePath modulePath) {
    return new PreludeResourceSource();
  }
}
