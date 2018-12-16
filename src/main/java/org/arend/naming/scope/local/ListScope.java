package org.arend.naming.scope.local;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.ImportedScope;
import org.arend.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class ListScope implements Scope {
  private final Scope myParent;
  private final List<? extends Referable> myReferables;

  public ListScope(Scope parent, List<? extends Referable> referables) {
    myParent = parent;
    myReferables = referables;
  }

  private Referable findHere(Predicate<Referable> pred) {
    for (int i = myReferables.size() - 1; i >= 0; i--) {
      Referable referable = myReferables.get(i);
      if (referable != null && pred.test(referable)) {
        return referable;
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    Referable ref = findHere(pred);
    return ref != null ? ref : myParent.find(pred);
  }

  @Override
  public Referable resolveName(String name) {
    Referable ref = findHere(ref2 -> ref2.textRepresentation().equals(name));
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return myParent.resolveNamespace(name, onlyInternal);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }

  @Nonnull
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
