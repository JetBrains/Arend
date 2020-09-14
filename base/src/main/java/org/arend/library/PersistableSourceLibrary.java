package org.arend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.term.group.ChildGroup;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a library which cannot be modified after loading.
 */
public abstract class PersistableSourceLibrary extends UnmodifiableSourceLibrary {
  private boolean myExternal = false;
  private final Set<ModulePath> myUpdatedModules = new LinkedHashSet<>();

  /**
   * Creates a new {@code UnmodifiableSourceLibrary}
   *
   * @param name              the name of this library.
   */
  protected PersistableSourceLibrary(String name) {
    super(name);
  }

  @Override
  public void groupLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw, boolean inTests) {
    super.groupLoaded(modulePath, group, isRaw, inTests);
    if (isRaw) {
      if (group == null) {
        myUpdatedModules.remove(modulePath);
      } else {
        myUpdatedModules.add(modulePath);
      }
    }
  }

  @Override
  public void binaryLoaded(ModulePath modulePath, boolean isComplete) {
    if (isComplete) {
      myUpdatedModules.remove(modulePath);
    }
  }

  @Override
  public boolean unload() {
    super.unload();
    myUpdatedModules.clear();
    return true;
  }

  @Override
  public void reset() {
    super.reset();
    myUpdatedModules.addAll(getLoadedModules());
  }

  @Override
  public Collection<? extends ModulePath> getUpdatedModules() {
    return myUpdatedModules;
  }

  public void updateModule(ModulePath module) {
    myUpdatedModules.add(module);
  }

  public void updateModules(Collection<? extends ModulePath> modules) {
    myUpdatedModules.addAll(modules);
  }

  public void clearUpdateModules() {
    myUpdatedModules.clear();
  }

  @Override
  public boolean persistUpdatedModules(ErrorReporter errorReporter) {
    boolean ok = super.persistUpdatedModules(errorReporter);
    myUpdatedModules.clear();
    return ok;
  }

  @Override
  public boolean isExternal() {
    return myExternal;
  }

  public void setExternal(boolean isExternal) {
    myExternal = isExternal;
  }
}
