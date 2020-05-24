package org.arend.naming.scope.local;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.ImportedScope;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public class ElimScope implements Scope {
  private final Scope myParent;
  private final Set<? extends Referable> myExcluded;

  public ElimScope(Scope parent, Set<? extends Referable> excluded) {
    myParent = parent;
    myExcluded = excluded;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    return myParent.find(ref -> !myExcluded.contains(ref) && pred.test(ref));
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = myParent.resolveName(name);
    return ref != null && myExcluded.contains(ref) ? null : ref;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return myParent.resolveNamespace(name, onlyInternal);
  }

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return myParent.getGlobalSubscopeWithoutOpens();
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
