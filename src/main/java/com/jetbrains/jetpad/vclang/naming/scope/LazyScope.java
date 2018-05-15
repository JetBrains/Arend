package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LazyScope implements Scope {
  private Scope myScope;
  private final Supplier<Scope> mySupplier;

  public LazyScope(Supplier<Scope> supplier) {
    mySupplier = supplier;
  }

  private void updateScope() {
    if (myScope == null) {
      myScope = mySupplier.get();
    }
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    updateScope();
    return myScope.getElements();
  }

  @Override
  public Referable resolveName(String name) {
    updateScope();
    return myScope.resolveName(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    updateScope();
    return myScope.resolveNamespace(name);
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    updateScope();
    return myScope.find(pred);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    updateScope();
    return myScope.getGlobalSubscope();
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    updateScope();
    return myScope.getGlobalSubscopeWithoutOpens();
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    updateScope();
    return myScope.getImportedSubscope();
  }
}
