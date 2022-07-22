package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @Override
  public @NotNull Collection<? extends Referable> getElements(Referable.RefKind kind) {
    updateScope();
    return myScope.getElements(kind);
  }

  @Override
  public Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    updateScope();
    return myScope.resolveName(name, kind);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    updateScope();
    return myScope.resolveNamespace(name, onlyInternal);
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    updateScope();
    return myScope.find(pred);
  }

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    updateScope();
    return myScope.getGlobalSubscope();
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    updateScope();
    return myScope.getGlobalSubscopeWithoutOpens(withImports);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    updateScope();
    return myScope.getImportedSubscope();
  }
}
