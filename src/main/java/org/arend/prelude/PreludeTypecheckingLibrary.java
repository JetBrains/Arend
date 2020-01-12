package org.arend.prelude;

import org.arend.ext.module.ModulePath;
import org.arend.library.LibraryManager;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;

import java.util.Collection;
import java.util.Collections;

/**
 * A library which is used to load and typecheck prelude.
 */
public abstract class PreludeTypecheckingLibrary extends PreludeLibrary {
  private boolean myTypechecked = false;

  /**
   * Creates a new {@code PreludeTypecheckingLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  public PreludeTypecheckingLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public boolean load(LibraryManager libraryManager, TypecheckingOrderingListener typechecking) {
    synchronized (PreludeLibrary.class) {
      if (getPreludeScope() == null) {
        return super.load(libraryManager, typechecking);
      }
    }

    myTypechecked = true;
    Prelude.fillInTypecheckerState(getTypecheckerState());
    setLoaded();
    return true;
  }

  @Override
  public Collection<? extends ModulePath> getUpdatedModules() {
    return myTypechecked ? Collections.emptyList() : Collections.singleton(Prelude.MODULE_PATH);
  }

  @Override
  public boolean needsTypechecking() {
    return !myTypechecked;
  }

  @Override
  public boolean orderModules(Ordering ordering) {
    if (myTypechecked) {
      return true;
    }

    if (super.orderModules(ordering)) {
      synchronized (PreludeLibrary.class) {
        if (!Prelude.isInitialized()) {
          Prelude.initialize(getPreludeScope(), getTypecheckerState());
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
