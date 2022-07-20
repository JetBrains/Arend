package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ListScope implements Scope {
  private final List<? extends Referable> myContext;
  private final List<? extends Referable> myPLevels;
  private final List<? extends Referable> myHLevels;

  public ListScope(List<? extends Referable> context, List<? extends Referable> pLevels, List<? extends Referable> hLevels) {
    myContext = context;
    myPLevels = pLevels;
    myHLevels = hLevels;
  }

  public ListScope(List<? extends Referable> context) {
    myContext = context;
    myPLevels = Collections.emptyList();
    myHLevels = Collections.emptyList();
  }

  public ListScope(Referable... context) {
    this(Arrays.asList(context));
  }

  @NotNull
  @Override
  public List<? extends Referable> getElements(Referable.RefKind kind) {
    return kind == Referable.RefKind.EXPR ? myContext : kind == Referable.RefKind.PLEVEL ? myPLevels : myHLevels;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (pred.test(myContext.get(i))) {
        return myContext.get(i);
      }
    }
    for (int i = myPLevels.size() - 1; i >= 0; i--) {
      if (pred.test(myPLevels.get(i))) {
        return myPLevels.get(i);
      }
    }
    for (int i = myHLevels.size() - 1; i >= 0; i--) {
      if (pred.test(myHLevels.get(i))) {
        return myHLevels.get(i);
      }
    }
    return null;
  }

  @Override
  public @Nullable Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    if (kind == null) {
      for (Referable.RefKind refKind : Referable.RefKind.values()) {
        Referable ref = resolveName(name, refKind);
        if (ref != null) return ref;
      }
    } else {
      List<? extends Referable> list = kind == Referable.RefKind.EXPR ? myContext : kind == Referable.RefKind.PLEVEL ? myPLevels : myHLevels;
      for (int i = list.size() - 1; i >= 0; i--) {
        if (list.get(i).getRefName().equals(name)) {
          return list.get(i);
        }
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    return EmptyScope.INSTANCE;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return EmptyScope.INSTANCE;
  }
}
