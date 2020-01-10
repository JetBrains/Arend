package org.arend.prelude;

import org.arend.ext.module.ModulePath;
import org.arend.library.LibraryManager;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.source.BinarySource;
import org.arend.source.GZIPStreamBinarySource;
import org.arend.source.Source;
import org.arend.typechecking.TypecheckerState;

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
          if (!Prelude.isInitialized()) {
            Prelude.initialize(getPreludeScope(), getTypecheckerState());
          }
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

  @Override
  public boolean supportsTypechecking() {
    return false;
  }

  @Override
  public boolean needsTypechecking() {
    return false;
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
