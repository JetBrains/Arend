package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.*;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A library which is used to load and persist prelude from and to a file.
 */
public class PreludeFileLibrary extends PreludeLibrary {
  /**
   * Creates a new {@code PreludeFileLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  PreludeFileLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
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
  public PersistableSource getBinarySource(ModulePath modulePath) {
    if (!modulePath.equals(Prelude.MODULE_PATH)) {
      return null;
    }
    return new GZIPStreamBinarySource(new FileBinarySource(PreludeResourceSource.BASE_PATH, Prelude.MODULE_PATH));
  }

  @Override
  public Collection<? extends ModulePath> getUpdatedModules() {
    return Collections.singleton(Prelude.MODULE_PATH);
  }
}
