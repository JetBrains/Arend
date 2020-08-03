package org.arend.prelude;

import org.arend.ext.module.ModulePath;
import org.arend.library.LibraryManager;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.source.BinarySource;
import org.arend.source.GZIPStreamBinarySource;
import org.arend.source.Source;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.Nullable;

/**
 * A library which is used to load prelude from a resource.
 */
public class PreludeResourceLibrary extends PreludeLibrary {
  @Override
  public boolean load(LibraryManager libraryManager, TypecheckingOrderingListener typechecking) {
    synchronized (PreludeLibrary.class) {
      if (getPreludeScope() == null) {
        if (super.load(libraryManager, typechecking)) {
          if (!Prelude.isInitialized()) {
            Prelude.initialize(getPreludeScope());
          }
          return true;
        } else {
          return false;
        }
      }
    }

    setLoaded();
    return true;
  }

  @Nullable
  @Override
  public Source getRawSource(ModulePath modulePath) {
    return null;
  }

  @Override
  public boolean isExternal() {
    return true;
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
}
